package com.synapse.waypoint.auth;

import static org.hamcrest.Matchers.nullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.auth.repository.UserAccountRepository;
import com.synapse.waypoint.common.seed.SeedStep;

import java.util.List;

/** The four demo accounts exist and can sign in. Outlets and vehicles come from invented CSVs. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.data-dir=src/test/resources/seed")
@Transactional
class DemoAccountsSeedStepTests {

    @Autowired MockMvc mvc;
    @Autowired List<SeedStep> steps;
    @Autowired UserAccountRepository users;

    @BeforeEach
    void seedReferenceDataAndAccounts() {
        steps.stream().filter(step -> step.order() == 10).forEach(SeedStep::run);
        steps.stream().filter(step -> step.order() == 40).forEach(SeedStep::run);
    }

    @Test
    void shouldSignInStoreManagerForTheDemoOutlet() throws Exception {
        mvc.perform(login("dilani@waypoint.lk", "Relay@2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("STORE_MANAGER"))
                .andExpect(jsonPath("$.user.outletId").value("OUT001"));
    }

    @Test
    void shouldSignInDispatcherWithoutDepotRestriction() throws Exception {
        mvc.perform(login("ruwan@waypoint.lk", "Relay@2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("DISPATCHER"))
                .andExpect(jsonPath("$.user.depot").value(nullValue()));
    }

    @Test
    void shouldSignInLoaderWithDepotAndPin() throws Exception {
        mvc.perform(login("Peliyagoda", "1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("LOADER"))
                .andExpect(jsonPath("$.user.depot").value("Peliyagoda"));
    }

    @Test
    void shouldSignInDriverForTheDemoVehicle() throws Exception {
        mvc.perform(login("DRV-0036", "3636"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("DRIVER"))
                .andExpect(jsonPath("$.user.vehicleId").value("VEH036"));
    }

    @Test
    void shouldStoreSecretsAsBcryptHashes() {
        assertThat(users.findAll()).hasSize(4)
                .allSatisfy(user -> assertThat(user.getSecretHash()).startsWith("$2"));
    }

    private MockHttpServletRequestBuilder login(String identifier, String secret) {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"%s\",\"secret\":\"%s\"}".formatted(identifier, secret));
    }
}
