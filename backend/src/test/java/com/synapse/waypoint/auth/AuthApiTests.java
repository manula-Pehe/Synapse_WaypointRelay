package com.synapse.waypoint.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** Sign-in for all three methods, the profile endpoints and the role rules. Test data is invented. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiTests {

    private static final String PASSWORD = "Test-Password-1";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordEncoder encoder;

    @BeforeEach
    void createTestAccounts() {
        jdbc.update("""
                INSERT INTO outlets (id, brand, district, depot, dock_type, parking_constraint, window_open, window_close)
                VALUES ('OUT900', 'Fresh', 'Testdistrict', 'Peliyagoda', 'rear_dock', 'normal', '05:00', '07:00')""");
        jdbc.update("""
                INSERT INTO vehicles (id, type, temp, weight_cap_kg, volume_cap_m3, fuel_type, km_per_l, weekly_fuel_quota_l, depot)
                VALUES ('VEH900', 'van', 'reefer', 1000, 6.0, 'diesel', 8.0, 300, 'Peliyagoda')""");
        insertUser("usr-t-store", "STORE_MANAGER", "store.test@example.lk", null, PASSWORD, "OUT900", null, null, true);
        insertUser("usr-t-dispatch", "DISPATCHER", "dispatch.test@example.lk", null, PASSWORD, null, null, null, true);
        insertUser("usr-t-loader", "LOADER", null, null, "4321", null, "Peliyagoda", null, true);
        insertUser("usr-t-driver", "DRIVER", null, "DRV-0900", "9090", null, null, "VEH900", true);
        insertUser("usr-t-gone", "DISPATCHER", "gone.test@example.lk", null, PASSWORD, null, null, null, false);
    }

    @Test
    void shouldSignInStoreManagerWithEmailAndPassword() throws Exception {
        mvc.perform(login("STORE.test@example.lk", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("STORE_MANAGER"))
                .andExpect(jsonPath("$.user.outletId").value("OUT900"));
    }

    @Test
    void shouldSignInDriverWithStaffIdAndPin() throws Exception {
        mvc.perform(login("DRV-0900", "9090"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.vehicleId").value("VEH900"));
    }

    @Test
    void shouldSignInLoaderWithDepotAndPin() throws Exception {
        mvc.perform(login("peliyagoda", "4321"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value("usr-t-loader"));
    }

    @Test
    void shouldRejectWrongSecretWithTheSameMessageAsUnknownUser() throws Exception {
        String wrongSecret = mvc.perform(login("store.test@example.lk", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andReturn().getResponse().getContentAsString();
        String unknownUser = mvc.perform(login("nobody@example.lk", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(wrongSecret).isEqualTo(unknownUser);
    }

    @Test
    void shouldRejectInactiveAccounts() throws Exception {
        mvc.perform(login("gone.test@example.lk", PASSWORD)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectEmptyFieldsAsValidationError() throws Exception {
        mvc.perform(login("", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    void shouldReturnAndUpdateTheSignedInUser() throws Exception {
        String token = tokenFor("store.test@example.lk", PASSWORD);

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("usr-t-store"));

        mvc.perform(patch("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"si\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("si"))
                .andExpect(jsonPath("$.theme").value("system"));
    }

    @Test
    void shouldRejectUnknownLanguage() throws Exception {
        String token = tokenFor("store.test@example.lk", PASSWORD);

        mvc.perform(patch("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"fr\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.language").exists());
    }

    @Test
    void shouldForbidAnotherRolesArea() throws Exception {
        String storeToken = tokenFor("store.test@example.lk", PASSWORD);

        mvc.perform(get("/api/dispatch/anything").header(HttpHeaders.AUTHORIZATION, bearer(storeToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/driver/anything").header(HttpHeaders.AUTHORIZATION, bearer(storeToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldLetARoleIntoItsOwnArea() throws Exception {
        String dispatcherToken = tokenFor("dispatch.test@example.lk", PASSWORD);

        // No endpoint exists yet, so passing security means 404 rather than 403.
        mvc.perform(get("/api/dispatch/anything").header(HttpHeaders.AUTHORIZATION, bearer(dispatcherToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldOnlyLetDispatchersMoveTheDemoClock() throws Exception {
        String storeToken = tokenFor("store.test@example.lk", PASSWORD);

        mvc.perform(post("/api/settings/clock").header(HttpHeaders.AUTHORIZATION, bearer(storeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"at\":\"2026-09-30T16:05:00+05:30\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectATamperedToken() throws Exception {
        String token = tokenFor("store.test@example.lk", PASSWORD);

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token + "x")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private String tokenFor(String identifier, String secret) throws Exception {
        MvcResult result = mvc.perform(login(identifier, secret)).andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"token\":\"") + "\"token\":\"".length();
        return body.substring(start, body.indexOf('"', start));
    }

    private static org.springframework.test.web.servlet.RequestBuilder login(String identifier, String secret) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + identifier + "\",\"secret\":\"" + secret + "\"}");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private void insertUser(String id, String role, String email, String staffId, String secret,
            String outletId, String depot, String vehicleId, boolean active) {
        jdbc.update("""
                INSERT INTO users (id, name, role, email, staff_id, secret_hash, outlet_id, depot, vehicle_id, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                id, "Test " + role, role, email, staffId, encoder.encode(secret), outletId, depot, vehicleId, active);
    }
}
