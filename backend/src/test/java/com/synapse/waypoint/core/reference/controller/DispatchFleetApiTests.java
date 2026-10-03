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

/** The fleet view (D2) over HTTP. Data is invented. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DispatchFleetApiTests {

    private static final String PASSWORD = "Test-Password-1";
    private static final String DEPOT = ReferenceFixtures.DEPOT;
    private static final String FLEET_URL = "/api/dispatch/fleet?runDate=2026-10-01&depot=" + DEPOT;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void createData() {
        OrderFixtures.insertOutlet(jdbc, "OUT991", "Fresh", "Testdistrict");
        OrderFixtures.insertOutlet(jdbc, "OUT993", "Fresh", "Otherdistrict", ReferenceFixtures.OTHER_DEPOT);
        ReferenceFixtures.insertVehicle(jdbc, "VEH991", "reefer", DEPOT);
        ReferenceFixtures.insertVehicle(jdbc, "VEH992", "reefer", DEPOT);
        ReferenceFixtures.insertVehicle(jdbc, "VEH993", "ambient", DEPOT);
        ReferenceFixtures.insertVehicle(jdbc, "VEH994", "ambient", DEPOT);
        ReferenceFixtures.insertVehicle(jdbc, "VEH995", "reefer", ReferenceFixtures.OTHER_DEPOT);
        ReferenceFixtures.insertAvailability(jdbc, "VEH992", ReferenceFixtures.RUN_DATE, "IN_WORKSHOP", "Service");
        ReferenceFixtures.insertAvailability(jdbc, "VEH993", ReferenceFixtures.RUN_DATE, "OFF_ROAD", "Brake issue");
        insertUser("usr-t-store", "STORE_MANAGER", "store.test@example.lk", "OUT991", null);
        insertUser("usr-t-dispatch", "DISPATCHER", "dispatch.test@example.lk", null, null);
        insertUser("usr-t-dispatch-d", "DISPATCHER", "depot.dispatch.test@example.lk", null, DEPOT);
    }

    @Test
    void shouldShowTheDepotsVehiclesWithAvailabilityAndCounts() throws Exception {
        mvc.perform(get(FLEET_URL).header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(4))
                .andExpect(jsonPath("$.items[1].id").value("VEH992"))
                .andExpect(jsonPath("$.items[1].availability").value("IN_WORKSHOP"))
                .andExpect(jsonPath("$.counts.available").value(2))
                .andExpect(jsonPath("$.counts.inWorkshop").value(1))
                .andExpect(jsonPath("$.counts.offRoad").value(1))
                .andExpect(jsonPath("$.counts.reeferAvailable").value(1))
                .andExpect(jsonPath("$.confirmedAt").doesNotExist())
                .andExpect(jsonPath("$.confirmedBy").doesNotExist());
    }

    @Test
    void shouldDefaultToTheDispatchersOwnDepotAndTheCurrentRunDate() throws Exception {
        mvc.perform(get("/api/dispatch/fleet").header(HttpHeaders.AUTHORIZATION, depotDispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(4));
    }

    @Test
    void shouldAskForADepotWhenTheDispatcherCoversEveryDepot() throws Exception {
        mvc.perform(get("/api/dispatch/fleet").header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    void shouldHideAnotherDepotFromADispatcherWhoHasOne() throws Exception {
        mvc.perform(get("/api/dispatch/fleet?runDate=2026-10-01&depot=" + ReferenceFixtures.OTHER_DEPOT)
                        .header(HttpHeaders.AUTHORIZATION, depotDispatcher()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldLetADispatcherWithoutADepotSeeAnyDepot() throws Exception {
        mvc.perform(get("/api/dispatch/fleet?runDate=2026-10-01&depot=otherdepot")
                        .header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.counts.reeferAvailable").value(1));
    }

    @Test
    void shouldReturn404ForAnUnknownDepot() throws Exception {
        mvc.perform(get("/api/dispatch/fleet?depot=Nowhere").header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldKeepTheFleetFromStoreManagersAndAnonymousUsers() throws Exception {
        mvc.perform(get(FLEET_URL).header(HttpHeaders.AUTHORIZATION, store())).andExpect(status().isForbidden());
        mvc.perform(get(FLEET_URL)).andExpect(status().isUnauthorized());
    }

    private String dispatcher() throws Exception {
        return ApiSignIn.bearer(mvc, "dispatch.test@example.lk", PASSWORD);
    }

    private String depotDispatcher() throws Exception {
        return ApiSignIn.bearer(mvc, "depot.dispatch.test@example.lk", PASSWORD);
    }

    private String store() throws Exception {
        return ApiSignIn.bearer(mvc, "store.test@example.lk", PASSWORD);
    }

    private void insertUser(String id, String role, String email, String outletId, String depot) {
        jdbc.update("""
                INSERT INTO users (id, name, role, email, secret_hash, outlet_id, depot, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, true)""", id, "Test " + role, role, email, encoder.encode(PASSWORD),
                outletId, depot);
    }
}
