package com.synapse.waypoint.core.reference.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

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

import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.support.ApiSignIn;
import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.reference.support.ReferenceFixtures;

/** Changing vehicle availability (D2v) and confirming the fleet (D2) over HTTP. Data is invented. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FleetChangesApiTests {

    private static final String PASSWORD = "Test-Password-1";
    private static final String DEPOT = ReferenceFixtures.DEPOT;
    private static final String CONFIRM_BODY = "{\"runDate\":\"2026-10-01\",\"depot\":\"" + DEPOT + "\"}";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired DemoClock clock;

    @BeforeEach
    void createData() {
        OrderFixtures.insertOutlet(jdbc, "OUT991", "Fresh", "Testdistrict");
        OrderFixtures.insertOutlet(jdbc, "OUT993", "Fresh", "Otherdistrict", ReferenceFixtures.OTHER_DEPOT);
        ReferenceFixtures.insertVehicle(jdbc, "VEH991", "reefer", DEPOT);
        ReferenceFixtures.insertVehicle(jdbc, "VEH995", "reefer", ReferenceFixtures.OTHER_DEPOT);
        insertUser("usr-t-store", "STORE_MANAGER", "store.test@example.lk", "OUT991", null);
        insertUser("usr-t-dispatch", "DISPATCHER", "dispatch.test@example.lk", null, null);
        insertUser("usr-t-dispatch-d", "DISPATCHER", "depot.dispatch.test@example.lk", null, DEPOT);
    }

    @Test
    void shouldTakeAVehicleOffTheRoadWithAReason() throws Exception {
        mvc.perform(update("VEH991", "OFF_ROAD", "Brake issue", dispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("VEH991"))
                .andExpect(jsonPath("$.availability").value("OFF_ROAD"))
                .andExpect(jsonPath("$.availabilityReason").value("Brake issue"));

        mvc.perform(get("/api/dispatch/fleet?runDate=2026-10-01&depot=" + DEPOT)
                        .header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(jsonPath("$.counts.offRoad").value(1))
                .andExpect(jsonPath("$.counts.reeferAvailable").value(0));
    }

    @Test
    void shouldRecordWhoChangedItAndWhenUsingTheDemoClock() throws Exception {
        mvc.perform(update("VEH991", "IN_WORKSHOP", "Service", dispatcher())).andExpect(status().isOk());

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT updated_by, updated_at FROM vehicle_availability WHERE vehicle_id = 'VEH991'");
        Instant updatedAt = ((java.sql.Timestamp) row.get("updated_at")).toInstant();
        assertThat(row.get("updated_by")).isEqualTo("usr-t-dispatch");
        assertThat(Duration.between(updatedAt, clock.now()).abs()).isLessThan(Duration.ofMinutes(1));
    }

    @Test
    void shouldPutAVehicleBackOnTheRoadAndDropTheReason() throws Exception {
        mvc.perform(update("VEH991", "OFF_ROAD", "Brake issue", dispatcher())).andExpect(status().isOk());

        mvc.perform(update("VEH991", "AVAILABLE", "ignored", dispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.availabilityReason").doesNotExist());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vehicle_availability WHERE vehicle_id = 'VEH991'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void shouldRequireAReasonUnlessTheVehicleIsAvailable() throws Exception {
        mvc.perform(update("VEH991", "OFF_ROAD", null, dispatcher()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.details.reason").exists());
        mvc.perform(update("VEH991", "IN_WORKSHOP", "   ", dispatcher())).andExpect(status().isBadRequest());
        mvc.perform(update("VEH991", "AVAILABLE", null, dispatcher())).andExpect(status().isOk());
    }

    @Test
    void shouldRejectAMissingStatusOrDate() throws Exception {
        mvc.perform(put("/api/dispatch/fleet/VEH991").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"runDate\":\"2026-10-01\"}").header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/dispatch/fleet/VEH991").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"AVAILABLE\"}").header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404ForAnUnknownVehicle() throws Exception {
        mvc.perform(update("VEH000", "AVAILABLE", null, dispatcher()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldReturn404ForAVehicleOfAnotherDepotWhenTheDispatcherHasADepot() throws Exception {
        mvc.perform(update("VEH995", "OFF_ROAD", "Brake issue", depotDispatcher()))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vehicle_availability WHERE vehicle_id = 'VEH995'",
                Integer.class)).isZero();
    }

    @Test
    void shouldLetADispatcherWithoutADepotChangeAnyDepotsVehicle() throws Exception {
        mvc.perform(update("VEH995", "OFF_ROAD", "Brake issue", dispatcher())).andExpect(status().isOk());
    }

    @Test
    void shouldLetADispatcherWithADepotChangeTheirOwnVehicle() throws Exception {
        mvc.perform(update("VEH991", "OFF_ROAD", "Brake issue", depotDispatcher())).andExpect(status().isOk());
    }

    @Test
    void shouldConfirmTheFleetAndWriteTheRunRow() throws Exception {
        mvc.perform(confirm(CONFIRM_BODY, dispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedAt").isNotEmpty())
                .andExpect(jsonPath("$.confirmedBy").value("usr-t-dispatch"));

        assertThat(jdbc.queryForObject("""
                SELECT fleet_confirmed_by FROM order_runs WHERE run_date = '2026-10-01' AND depot = ?""",
                String.class, DEPOT)).isEqualTo("usr-t-dispatch");
        mvc.perform(get("/api/dispatch/fleet?runDate=2026-10-01&depot=" + DEPOT)
                        .header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(jsonPath("$.confirmedAt").isNotEmpty())
                .andExpect(jsonPath("$.confirmedBy").value("usr-t-dispatch"));
    }

    @Test
    void shouldAllowConfirmingTwiceAndKeepOneRunRow() throws Exception {
        mvc.perform(confirm(CONFIRM_BODY, dispatcher())).andExpect(status().isOk());

        mvc.perform(confirm(CONFIRM_BODY, depotDispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedBy").value("usr-t-dispatch-d"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM order_runs WHERE depot = ?", Integer.class, DEPOT))
                .isEqualTo(1);
    }

    @Test
    void shouldKeepTheOrderCutOffWhenConfirmingTheFleet() throws Exception {
        mvc.perform(post("/api/dispatch/orders/close").contentType(MediaType.APPLICATION_JSON).content(CONFIRM_BODY)
                .header(HttpHeaders.AUTHORIZATION, dispatcher())).andExpect(status().isOk());

        mvc.perform(confirm(CONFIRM_BODY, dispatcher())).andExpect(status().isOk());

        mvc.perform(get("/api/orders/close-status?runDate=2026-10-01&depot=" + DEPOT)
                        .header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(jsonPath("$.closed").value(true));
    }

    @Test
    void shouldHideAnotherDepotWhenConfirmingAsADepotDispatcher() throws Exception {
        mvc.perform(confirm("{\"runDate\":\"2026-10-01\",\"depot\":\"" + ReferenceFixtures.OTHER_DEPOT + "\"}",
                depotDispatcher())).andExpect(status().isNotFound());
    }

    @Test
    void shouldReject400WhenConfirmingAnUnknownDepot() throws Exception {
        mvc.perform(confirm("{\"runDate\":\"2026-10-01\",\"depot\":\"Nowhere\"}", dispatcher()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM order_runs WHERE depot = 'Nowhere'", Integer.class))
                .isZero();
    }

    @Test
    void shouldRejectAConfirmWithoutADepot() throws Exception {
        mvc.perform(confirm("{\"runDate\":\"2026-10-01\"}", dispatcher())).andExpect(status().isBadRequest());
    }

    @Test
    void shouldKeepFleetChangesFromStoreManagersAndAnonymousUsers() throws Exception {
        mvc.perform(update("VEH991", "AVAILABLE", null, store())).andExpect(status().isForbidden());
        mvc.perform(confirm(CONFIRM_BODY, store())).andExpect(status().isForbidden());
        mvc.perform(post("/api/dispatch/fleet/confirm").contentType(MediaType.APPLICATION_JSON).content(CONFIRM_BODY))
                .andExpect(status().isUnauthorized());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder update(
            String vehicleId, String status, String reason, String bearer) {
        String reasonJson = reason == null ? "" : ",\"reason\":\"" + reason + "\"";
        return put("/api/dispatch/fleet/" + vehicleId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"runDate\":\"2026-10-01\",\"status\":\"" + status + "\"" + reasonJson + "}")
                .header(HttpHeaders.AUTHORIZATION, bearer);
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder confirm(String body,
            String bearer) {
        return post("/api/dispatch/fleet/confirm").contentType(MediaType.APPLICATION_JSON).content(body)
                .header(HttpHeaders.AUTHORIZATION, bearer);
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
