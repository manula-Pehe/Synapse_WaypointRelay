package com.synapse.waypoint.planning.controller;

import static com.synapse.waypoint.planning.support.PlanScenario.DEPOT;
import static com.synapse.waypoint.planning.support.PlanScenario.PASSWORD;
import static com.synapse.waypoint.planning.support.PlanScenario.PIN;
import static com.synapse.waypoint.planning.support.PlanScenario.RUN_DATE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.service.CloseOrdersService;
import com.synapse.waypoint.core.order.support.ApiSignIn;
import com.synapse.waypoint.planning.support.PlanScenario;

/** The dispatcher plan endpoints over HTTP. Data is invented. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DispatchPlanApiTests {

    private static final String PLANS = "/api/dispatch/plans";
    private static final String QUERY = "?runDate=" + RUN_DATE + "&depot=" + DEPOT;
    private static final String CREATE_BODY = "{\"runDate\":\"" + RUN_DATE + "\",\"depot\":\"" + DEPOT + "\"}";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired CloseOrdersService closeOrders;

    @BeforeEach
    void createDepotDay() {
        PlanScenario.insert(jdbc, encoder);
    }

    @Test
    void shouldReportWhatIsStillMissingBeforeAPlanCanBeCreated() throws Exception {
        mvc.perform(get(PLANS + "/readiness" + QUERY).header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordersClosed").value(false))
                .andExpect(jsonPath("$.fleetConfirmed").value(false))
                .andExpect(jsonPath("$.confirmedOrders").value(3))
                .andExpect(jsonPath("$.availableVehicles").value(1))
                .andExpect(jsonPath("$.reeferAvailable").value(1))
                .andExpect(jsonPath("$.warnings.length()").value(2));
    }

    @Test
    void shouldCreateAPlanWithTheDocumentedShape() throws Exception {
        closeOrders.close(RUN_DATE, DEPOT);

        mvc.perform(createPlan(dispatcher()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.summary.served").value(2))
                .andExpect(jsonPath("$.summary.deferred").value(1))
                .andExpect(jsonPath("$.summary.warnings").isNumber())
                .andExpect(jsonPath("$.vehicles[0].vehicleId").value(PlanScenario.USED_VEHICLE))
                .andExpect(jsonPath("$.vehicles[0].freshBudget").value(270))
                .andExpect(jsonPath("$.vehicles[0].daytimeBudget").value(480))
                .andExpect(jsonPath("$.vehicles[0].trips[0].windowType").value("FRESH"))
                .andExpect(jsonPath("$.vehicles[0].trips[0].stops[0].orderRef").value("PL-1"))
                .andExpect(jsonPath("$.vehicles[0].trips[0].stops[0].lateRisk").isNumber());
    }

    @Test
    void shouldRefuseToCreateBeforeOrdersAreClosed() throws Exception {
        mvc.perform(createPlan(dispatcher()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDERS_NOT_CLOSED"));
    }

    @Test
    void shouldReadTheLatestPlanItsDeferralsAndAnswer404WhenThereIsNone() throws Exception {
        mvc.perform(get(PLANS + QUERY).header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isNotFound());
        closeOrders.close(RUN_DATE, DEPOT);
        String planId = createdPlanId();

        mvc.perform(get(PLANS + QUERY).header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(planId));
        mvc.perform(get(PLANS + "/" + planId).header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(planId));
        mvc.perform(get(PLANS + "/" + planId + "/deferrals").header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].orderRef").value("PL-3"))
                .andExpect(jsonPath("$.items[0].kind").value("UNAVOIDABLE"))
                .andExpect(jsonPath("$.items[0].storeChoice").doesNotExist());
    }

    @Test
    void shouldPublishOnceAndRefuseTheSecondPublishWithPlanLocked() throws Exception {
        closeOrders.close(RUN_DATE, DEPOT);
        String planId = createdPlanId();

        mvc.perform(post(PLANS + "/" + planId + "/publish").header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(post(PLANS + "/" + planId + "/publish").header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAN_LOCKED"));
    }

    @Test
    void shouldRefuseCreatingAfterPublishWithPlanLocked() throws Exception {
        closeOrders.close(RUN_DATE, DEPOT);
        String planId = createdPlanId();
        mvc.perform(post(PLANS + "/" + planId + "/publish").header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isOk());

        mvc.perform(createPlan(dispatcher()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAN_LOCKED"));
    }

    @Test
    void shouldKeepEveryNonDispatcherRoleOutOfAllPlanEndpoints() throws Exception {
        closeOrders.close(RUN_DATE, DEPOT);
        String planId = createdPlanId();
        List<String> others = List.of(bearer("P-OUT1@example.lk", PASSWORD),
                bearer(DEPOT, PIN), bearer("DRV-9001", PIN));

        for (String token : others) {
            mvc.perform(get(PLANS + "/readiness" + QUERY).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mvc.perform(createPlan(token)).andExpect(status().isForbidden());
            mvc.perform(get(PLANS + QUERY).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mvc.perform(get(PLANS + "/" + planId).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mvc.perform(get(PLANS + "/" + planId + "/deferrals").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mvc.perform(post(PLANS + "/" + planId + "/publish").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get(PLANS + QUERY)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldHideAnotherDepotsPlanFromADispatcherWhoHasADepot() throws Exception {
        closeOrders.close(RUN_DATE, DEPOT);
        String planId = createdPlanId();
        String otherDepotDispatcher = bearer("other.dispatch.p@example.lk", PASSWORD);

        mvc.perform(get(PLANS + "/" + planId).header(HttpHeaders.AUTHORIZATION, otherDepotDispatcher))
                .andExpect(status().isNotFound());
        mvc.perform(post(PLANS + "/" + planId + "/publish").header(HttpHeaders.AUTHORIZATION, otherDepotDispatcher))
                .andExpect(status().isNotFound());
    }

    private MockHttpServletRequestBuilder createPlan(String token) {
        return post(PLANS).header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY);
    }

    private String createdPlanId() throws Exception {
        ResultActions created = mvc.perform(createPlan(dispatcher())).andExpect(status().isCreated());
        String body = created.andReturn().getResponse().getContentAsString();
        int start = body.indexOf("\"id\":\"") + "\"id\":\"".length();
        return body.substring(start, body.indexOf('"', start));
    }

    private String dispatcher() throws Exception {
        return bearer("dispatch.p@example.lk", PASSWORD);
    }

    private String bearer(String identifier, String secret) throws Exception {
        return ApiSignIn.bearer(mvc, identifier, secret);
    }
}
