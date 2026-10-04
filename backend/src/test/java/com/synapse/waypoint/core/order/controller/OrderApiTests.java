package com.synapse.waypoint.core.order.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.order.support.ApiSignIn;
import com.synapse.waypoint.core.order.support.OrderFixtures;

/** Order list and detail over HTTP, including each role's data scope. Data is invented. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderApiTests {

    private static final String PASSWORD = "Test-Password-1";
    private static final String OUTLET = "OUT991";
    private static final String OTHER_OUTLET = "OUT992";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired OrderRepository orders;
    @Autowired OrderService orderService;

    private Order preparedOrder;
    private Order confirmedOrder;
    private Order otherOutletOrder;

    @BeforeEach
    void createOutletsUsersAndOrders() {
        OrderFixtures.insertOutlet(jdbc, OUTLET, "Fresh", "Testdistrict");
        OrderFixtures.insertOutlet(jdbc, OTHER_OUTLET, "Fresh", "Otherdistrict");
        insertUser("usr-t-store", "STORE_MANAGER", "store.test@example.lk", OUTLET);
        insertUser("usr-t-dispatch", "DISPATCHER", "dispatch.test@example.lk", null);
        preparedOrder = OrderFixtures.save(orders, OUTLET, OrderStatus.PREPARED);
        confirmedOrder = OrderFixtures.save(orders, OUTLET, OrderStatus.CONFIRMED);
        otherOutletOrder = OrderFixtures.save(orders, OTHER_OUTLET, OrderStatus.CONFIRMED);
    }

    @Test
    void shouldRequireSignIn() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders/" + preparedOrder.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldListTheOrdersOfARunAndDepotForADispatcher() throws Exception {
        mvc.perform(get("/api/orders?runDate=2026-10-01&depot=testdepot").header(HttpHeaders.AUTHORIZATION, bearer("dispatch")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].outletName").isNotEmpty());
    }

    @Test
    void shouldFilterByStatusAndOutlet() throws Exception {
        mvc.perform(get("/api/orders?runDate=2026-10-01&depot=Testdepot&status=CONFIRMED&outletId=" + OTHER_OUTLET)
                .header(HttpHeaders.AUTHORIZATION, bearer("dispatch")))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(otherOutletOrder.getId()));
    }

    @Test
    void shouldListNothingForADifferentRunDateOrDepot() throws Exception {
        mvc.perform(get("/api/orders?runDate=2026-10-02&depot=Testdepot").header(HttpHeaders.AUTHORIZATION, bearer("dispatch")))
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/orders?runDate=2026-10-01&depot=Otherdepot").header(HttpHeaders.AUTHORIZATION, bearer("dispatch")))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void shouldLimitAStoreManagerToTheirOwnOutletEvenWhenAskingForAnother() throws Exception {
        mvc.perform(get("/api/orders?runDate=2026-10-01&depot=Testdepot&outletId=" + OTHER_OUTLET)
                .header(HttpHeaders.AUTHORIZATION, bearer("store")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[?(@.outletId != '" + OUTLET + "')]").isEmpty());
    }

    @Test
    void shouldReturnAnOrderWithItsHistory() throws Exception {
        orderService.confirm(preparedOrder.getId());

        mvc.perform(get("/api/orders/" + preparedOrder.getId()).header(HttpHeaders.AUTHORIZATION, bearer("store")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.id").value(preparedOrder.getId()))
                .andExpect(jsonPath("$.order.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.order.outletName").value("OUT991 · Testdistrict"))
                .andExpect(jsonPath("$.history.length()").value(1))
                .andExpect(jsonPath("$.history[0].type").value("CONFIRMED"))
                .andExpect(jsonPath("$.history[0].fromStatus").value("PREPARED"))
                .andExpect(jsonPath("$.history[0].toStatus").value("CONFIRMED"));
    }

    @Test
    void shouldReturn404ForAnotherOutletsOrderToAStoreManager() throws Exception {
        mvc.perform(get("/api/orders/" + otherOutletOrder.getId()).header(HttpHeaders.AUTHORIZATION, bearer("store")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldReturn404ForAnUnknownOrder() throws Exception {
        mvc.perform(get("/api/orders/missing").header(HttpHeaders.AUTHORIZATION, bearer("dispatch")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectAnUnknownStatusFilter() throws Exception {
        mvc.perform(get("/api/orders?status=SHIPPED").header(HttpHeaders.AUTHORIZATION, bearer("dispatch")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    private String bearer(String who) throws Exception {
        return ApiSignIn.bearer(mvc, who + ".test@example.lk", PASSWORD);
    }

    private void insertUser(String id, String role, String email, String outletId) {
        jdbc.update("""
                INSERT INTO users (id, name, role, email, secret_hash, outlet_id, active)
                VALUES (?, ?, ?, ?, ?, ?, true)""", id, "Test " + role, role, email, encoder.encode(PASSWORD), outletId);
    }
}
