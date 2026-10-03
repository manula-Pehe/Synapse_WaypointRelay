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
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.support.ApiSignIn;
import com.synapse.waypoint.core.order.support.OrderFixtures;

/** Close orders (D1) and close status over HTTP. Data is invented. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CloseOrdersApiTests {

    private static final String PASSWORD = "Test-Password-1";
    private static final String OUTLET = "OUT991";
    private static final String CLOSE_BODY = """
            {"runDate":"2026-10-01","depot":"Testdepot"}""";
    private static final String STATUS_URL = "/api/orders/close-status?runDate=2026-10-01&depot=Testdepot";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired OrderRepository orders;

    @BeforeEach
    void createOutletAndUsers() {
        OrderFixtures.insertOutlet(jdbc, OUTLET, "Fresh", "Testdistrict");
        insertUser("usr-t-store", "STORE_MANAGER", "store.test@example.lk", OUTLET);
        insertUser("usr-t-dispatch", "DISPATCHER", "dispatch.test@example.lk", null);
    }

    @Test
    void shouldCloseOrdersAndReturnTheCounts() throws Exception {
        OrderFixtures.save(orders, OUTLET, OrderStatus.CONFIRMED);
        OrderFixtures.save(orders, OUTLET, "Fresh", TemperatureRequirement.AMBIENT, OrderStatus.PREPARED);
        OrderFixtures.save(orders, OUTLET, "Tech", TemperatureRequirement.AMBIENT, OrderStatus.PREPARED);

        mvc.perform(post("/api/dispatch/orders/close").contentType(MediaType.APPLICATION_JSON).content(CLOSE_BODY)
                        .header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(1))
                .andExpect(jsonPath("$.autoConfirmed").value(1))
                .andExpect(jsonPath("$.notConfirmed").value(1))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());
    }

    @Test
    void shouldRefuseASecondCloseWith409OrdersClosed() throws Exception {
        String dispatcher = dispatcher();
        mvc.perform(post("/api/dispatch/orders/close").contentType(MediaType.APPLICATION_JSON).content(CLOSE_BODY)
                .header(HttpHeaders.AUTHORIZATION, dispatcher)).andExpect(status().isOk());

        mvc.perform(post("/api/dispatch/orders/close").contentType(MediaType.APPLICATION_JSON).content(CLOSE_BODY)
                        .header(HttpHeaders.AUTHORIZATION, dispatcher))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDERS_CLOSED"));
    }

    @Test
    void shouldRejectACloseWithoutADepot() throws Exception {
        mvc.perform(post("/api/dispatch/orders/close").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"runDate\":\"2026-10-01\",\"depot\":\"\"}")
                        .header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    void shouldKeepTheCloseButtonFromStoreManagersAndAnonymousUsers() throws Exception {
        mvc.perform(post("/api/dispatch/orders/close").contentType(MediaType.APPLICATION_JSON).content(CLOSE_BODY)
                        .header(HttpHeaders.AUTHORIZATION, store()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/dispatch/orders/close").contentType(MediaType.APPLICATION_JSON).content(CLOSE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldShowAnOpenRunToAStoreManagerWithTheCutOffTime() throws Exception {
        mvc.perform(get(STATUS_URL).header(HttpHeaders.AUTHORIZATION, store()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(false))
                .andExpect(jsonPath("$.closedAt").doesNotExist())
                .andExpect(jsonPath("$.cutOffAt").value("2026-09-30T16:00:00+05:30"));
    }

    @Test
    void shouldShowAClosedRunToEveryRoleWithWhoClosedIt() throws Exception {
        mvc.perform(post("/api/dispatch/orders/close").contentType(MediaType.APPLICATION_JSON).content(CLOSE_BODY)
                .header(HttpHeaders.AUTHORIZATION, dispatcher())).andExpect(status().isOk());

        mvc.perform(get(STATUS_URL).header(HttpHeaders.AUTHORIZATION, store()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(true))
                .andExpect(jsonPath("$.closedAt").isNotEmpty())
                .andExpect(jsonPath("$.closedBy").value("usr-t-dispatch"))
                .andExpect(jsonPath("$.cutOffAt").value("2026-09-30T16:00:00+05:30"));
    }

    @Test
    void shouldRequireSignInForCloseStatus() throws Exception {
        mvc.perform(get(STATUS_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireADepotForCloseStatus() throws Exception {
        mvc.perform(get("/api/orders/close-status?runDate=2026-10-01").header(HttpHeaders.AUTHORIZATION, store()))
                .andExpect(status().isBadRequest());
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
