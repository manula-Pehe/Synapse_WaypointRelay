package com.synapse.waypoint.core.order.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.support.ApiSignIn;
import com.synapse.waypoint.core.order.support.OrderFixtures;

/** Phone-in (D1b) and unconfirmed (D1u) over HTTP. Data is invented. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DispatchOrderApiTests {

    private static final String PASSWORD = "Test-Password-1";
    private static final String OUTLET_A = "OUT991";
    private static final String OUTLET_B = "OUT992";
    private static final String PHONE_IN_BODY = """
            {"outletId":"OUT991","runDate":"2026-10-01","temp":"CHILLED","units":12,"note":"Phoned at 3 PM"}""";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired OrderRepository orders;

    @BeforeEach
    void createOutletsAndUsers() {
        OrderFixtures.insertOutlet(jdbc, OUTLET_A, "Fresh", "Testdistrict");
        OrderFixtures.insertOutlet(jdbc, OUTLET_B, "Style", "Otherdistrict");
        insertUser("usr-t-store", "STORE_MANAGER", "store.test@example.lk", OUTLET_A);
        insertUser("usr-t-dispatch", "DISPATCHER", "dispatch.test@example.lk", null);
    }

    @Test
    void shouldCreateAPhoneInOrderConfirmedAndNotYetCheckedByTheStore() throws Exception {
        mvc.perform(post("/api/dispatch/orders/phone-in").contentType(MediaType.APPLICATION_JSON)
                        .content(PHONE_IN_BODY).header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("PHONE_IN"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.storeChecked").value(false))
                .andExpect(jsonPath("$.temp").value("CHILLED"))
                .andExpect(jsonPath("$.units").value(12))
                .andExpect(jsonPath("$.outletName").value("OUT991 · Testdistrict"));
    }

    @Test
    void shouldRejectAPhoneInOrderWithoutUnits() throws Exception {
        mvc.perform(post("/api/dispatch/orders/phone-in").contentType(MediaType.APPLICATION_JSON)
                        .content(PHONE_IN_BODY.replace("\"units\":12", "\"units\":0"))
                        .header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.details.units").isNotEmpty());
    }

    @Test
    void shouldReturn404WhenThePhoneInOutletDoesNotExist() throws Exception {
        mvc.perform(post("/api/dispatch/orders/phone-in").contentType(MediaType.APPLICATION_JSON)
                        .content(PHONE_IN_BODY.replace("OUT991", "OUT000"))
                        .header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldKeepDispatchEndpointsFromStoreManagersAndAnonymousUsers() throws Exception {
        mvc.perform(post("/api/dispatch/orders/phone-in").contentType(MediaType.APPLICATION_JSON)
                        .content(PHONE_IN_BODY).header(HttpHeaders.AUTHORIZATION, store()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/dispatch/orders/unconfirmed")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldGroupPreparedOrdersByOutletAndLeaveOutConfirmedOnes() throws Exception {
        OrderFixtures.save(orders, OUTLET_B, OrderStatus.PREPARED);
        OrderFixtures.save(orders, OUTLET_A, OrderStatus.PREPARED);
        OrderFixtures.save(orders, OUTLET_A, OrderStatus.PREPARED);
        OrderFixtures.save(orders, OUTLET_A, OrderStatus.CONFIRMED);

        mvc.perform(get("/api/dispatch/orders/unconfirmed?runDate=2026-10-01&depot=Testdepot")
                        .header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].outletId").value(OUTLET_A))
                .andExpect(jsonPath("$.items[0].outletName").value("OUT991 · Testdistrict"))
                .andExpect(jsonPath("$.items[0].phone").doesNotExist())
                .andExpect(jsonPath("$.items[0].orders.length()").value(2))
                .andExpect(jsonPath("$.items[0].orders[?(@.status != 'PREPARED')]").isEmpty())
                .andExpect(jsonPath("$.items[1].outletId").value(OUTLET_B))
                .andExpect(jsonPath("$.items[1].orders.length()").value(1));
    }

    @Test
    void shouldReturnAnEmptyListWhenEveryoneHasConfirmed() throws Exception {
        OrderFixtures.save(orders, OUTLET_A, OrderStatus.CONFIRMED);

        mvc.perform(get("/api/dispatch/orders/unconfirmed?runDate=2026-10-01&depot=Testdepot")
                        .header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    private String dispatcher() throws Exception {
        return ApiSignIn.bearer(mvc, "dispatch.test@example.lk", PASSWORD);
    }

    private String store() throws Exception {
        return ApiSignIn.bearer(mvc, "store.test@example.lk", PASSWORD);
    }

    private void insertUser(String id, String role, String email, String outletId) {
        jdbc.update("""
                INSERT INTO users (id, name, role, email, secret_hash, outlet_id, active)
                VALUES (?, ?, ?, ?, ?, ?, true)""", id, "Test " + role, role, email, encoder.encode(PASSWORD), outletId);
    }
}
