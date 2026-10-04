package com.synapse.waypoint.planning.controller;

import static com.synapse.waypoint.planning.support.PlanScenario.DEPOT;
import static com.synapse.waypoint.planning.support.PlanScenario.HEAVY_STORE;
import static com.synapse.waypoint.planning.support.PlanScenario.PASSWORD;
import static com.synapse.waypoint.planning.support.PlanScenario.RUN_DATE;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.service.CloseOrdersService;
import com.synapse.waypoint.core.order.support.ApiSignIn;
import com.synapse.waypoint.core.order.support.SignedInUser;
import com.synapse.waypoint.planning.service.PlanQueryService;
import com.synapse.waypoint.planning.service.PlanService;
import com.synapse.waypoint.planning.support.PlanScenario;

/** POST /api/store/deferrals/{id}/choice over HTTP. Data is invented. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StoreDeferralApiTests {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired CloseOrdersService closeOrders;
    @Autowired PlanService plans;
    @Autowired PlanQueryService query;

    private String deferralId;

    @BeforeEach
    void publishAPlanThatDefersTheHeavyOrder() {
        PlanScenario.insert(jdbc, encoder);
        SignedInUser.asDispatcher(PlanScenario.DISPATCHER);
        closeOrders.close(RUN_DATE, DEPOT);
        plans.publish(plans.create(RUN_DATE, DEPOT).id());
        deferralId = query.deferralForOrder(PlanScenario.orderId("PL-3")).orElseThrow().id();
        SignedInUser.signOut();
    }

    @Test
    void shouldReturnTheDeferralWithTheChoiceInTheStoreShape() throws Exception {
        mvc.perform(choice(deferralId, "{\"choice\":\"REDUCE\",\"units\":4}", store(HEAVY_STORE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deferralId))
                .andExpect(jsonPath("$.kind").value("UNAVOIDABLE"))
                .andExpect(jsonPath("$.rule").isNotEmpty())
                .andExpect(jsonPath("$.reason").isNotEmpty())
                .andExpect(jsonPath("$.newDate").isNotEmpty())
                .andExpect(jsonPath("$.needsDecision").value(false))
                .andExpect(jsonPath("$.storeChoice").value("REDUCE"))
                .andExpect(jsonPath("$.splitOffered").value(true));
    }

    @Test
    void shouldAnswer409DuplicateToASecondChoice() throws Exception {
        mvc.perform(choice(deferralId, "{\"choice\":\"KEEP\"}", store(HEAVY_STORE))).andExpect(status().isOk());

        mvc.perform(choice(deferralId, "{\"choice\":\"CANCEL\"}", store(HEAVY_STORE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
    }

    @Test
    void shouldAnswer400ForAMissingChoiceAndForAReduceWithoutUnits() throws Exception {
        mvc.perform(choice(deferralId, "{}", store(HEAVY_STORE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
        mvc.perform(choice(deferralId, "{\"choice\":\"REDUCE\"}", store(HEAVY_STORE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    void shouldAnswer404ToAnotherOutletsManager() throws Exception {
        mvc.perform(choice(deferralId, "{\"choice\":\"CANCEL\"}", store(PlanScenario.STORE_1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldKeepTheEndpointToStoreManagers() throws Exception {
        String dispatcher = ApiSignIn.bearer(mvc, "dispatch.p@example.lk", PASSWORD);

        mvc.perform(choice(deferralId, "{\"choice\":\"KEEP\"}", dispatcher)).andExpect(status().isForbidden());
        mvc.perform(post("/api/store/deferrals/" + deferralId + "/choice").contentType(MediaType.APPLICATION_JSON)
                .content("{\"choice\":\"KEEP\"}")).andExpect(status().isUnauthorized());
    }

    private static MockHttpServletRequestBuilder choice(String id, String body, String bearer) {
        return post("/api/store/deferrals/" + id + "/choice").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String store(String outletId) throws Exception {
        return ApiSignIn.bearer(mvc, outletId + "@example.lk", PASSWORD);
    }
}
