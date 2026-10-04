package com.synapse.waypoint.core.reference.controller;

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

import com.synapse.waypoint.core.order.support.ApiSignIn;
import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.reference.support.ReferenceFixtures;

/** Outlet and vehicle endpoints over HTTP. Data is invented. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReferenceApiTests {

    private static final String PASSWORD = "Test-Password-1";
    private static final String DEPOT = ReferenceFixtures.DEPOT;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void createData() {
        OrderFixtures.insertOutlet(jdbc, "OUT991", "Fresh", "Testdistrict");
        OrderFixtures.insertOutlet(jdbc, "OUT992", "Style", "Seconddistrict");
        OrderFixtures.insertOutlet(jdbc, "OUT993", "Fresh", "Otherdistrict", ReferenceFixtures.OTHER_DEPOT);
        ReferenceFixtures.insertVehicle(jdbc, "VEH991", "reefer", DEPOT);
        ReferenceFixtures.insertVehicle(jdbc, "VEH992", "ambient", DEPOT);
        ReferenceFixtures.insertAvailability(jdbc, "VEH992", ReferenceFixtures.RUN_DATE, "OFF_ROAD", "Brake issue");
        jdbc.update("""
                INSERT INTO users (id, name, role, email, secret_hash, outlet_id, active)
                VALUES ('usr-t-store', 'Test store', 'STORE_MANAGER', 'store.test@example.lk', ?, 'OUT991', true)""",
                encoder.encode(PASSWORD));
    }

    @Test
    void shouldListOutletsOfADepotForAStoreManager() throws Exception {
        mvc.perform(get("/api/outlets?depot=" + DEPOT).header(HttpHeaders.AUTHORIZATION, store()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].id").value("OUT991"))
                .andExpect(jsonPath("$.items[0].name").value("OUT991 · Testdistrict"))
                .andExpect(jsonPath("$.items[0].windowOpen").value("05:00"))
                .andExpect(jsonPath("$.items[0].mallWindowOpen").doesNotExist());
    }

    @Test
    void shouldFilterOutletsByBrand() throws Exception {
        mvc.perform(get("/api/outlets?depot=" + DEPOT + "&brand=Style").header(HttpHeaders.AUTHORIZATION, store()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value("OUT992"));
    }

    @Test
    void shouldReturnOneOutlet() throws Exception {
        mvc.perform(get("/api/outlets/OUT992").header(HttpHeaders.AUTHORIZATION, store()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand").value("Style"))
                .andExpect(jsonPath("$.depot").value(DEPOT));
    }

    @Test
    void shouldReturn404ForAnUnknownOutlet() throws Exception {
        mvc.perform(get("/api/outlets/OUT000").header(HttpHeaders.AUTHORIZATION, store()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldListVehiclesWithTheirAvailabilityOnTheRunDate() throws Exception {
        mvc.perform(get("/api/vehicles?depot=" + DEPOT + "&runDate=2026-10-01")
                        .header(HttpHeaders.AUTHORIZATION, store()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].id").value("VEH991"))
                .andExpect(jsonPath("$.items[0].temp").value("reefer"))
                .andExpect(jsonPath("$.items[0].availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.items[0].availabilityReason").doesNotExist())
                .andExpect(jsonPath("$.items[1].availability").value("OFF_ROAD"))
                .andExpect(jsonPath("$.items[1].availabilityReason").value("Brake issue"));
    }

    @Test
    void shouldTreatADateWithNoAvailabilityRowsAsAvailable() throws Exception {
        mvc.perform(get("/api/vehicles?depot=" + DEPOT + "&runDate=2026-10-02")
                        .header(HttpHeaders.AUTHORIZATION, store()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[1].availability").value("AVAILABLE"));
    }

    @Test
    void shouldRequireSignIn() throws Exception {
        mvc.perform(get("/api/outlets")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/outlets/OUT991")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/vehicles")).andExpect(status().isUnauthorized());
    }

    private String store() throws Exception {
        return ApiSignIn.bearer(mvc, "store.test@example.lk", PASSWORD);
    }
}
