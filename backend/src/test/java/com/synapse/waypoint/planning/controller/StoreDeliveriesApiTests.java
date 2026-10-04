package com.synapse.waypoint.planning.controller;

import static com.synapse.waypoint.planning.support.PlanScenario.DEPOT;
import static com.synapse.waypoint.planning.support.PlanScenario.HEAVY_STORE;
import static com.synapse.waypoint.planning.support.PlanScenario.PASSWORD;
import static com.synapse.waypoint.planning.support.PlanScenario.RUN_DATE;
import static com.synapse.waypoint.planning.support.PlanScenario.STORE_1;
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

import com.synapse.waypoint.core.order.service.CloseOrdersService;
import com.synapse.waypoint.core.order.support.ApiSignIn;
import com.synapse.waypoint.core.order.support.SignedInUser;
import com.synapse.waypoint.planning.service.PlanService;
import com.synapse.waypoint.planning.support.PlanScenario;

/** GET /api/store/deliveries over HTTP with a published plan. Data is invented. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StoreDeliveriesApiTests {

    private static final String DELIVERIES = "/api/store/deliveries?runDate=" + RUN_DATE;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired CloseOrdersService closeOrders;
    @Autowired PlanService plans;

    @BeforeEach
    void publishAPlan() {
        PlanScenario.insert(jdbc, encoder);
        SignedInUser.asDispatcher(PlanScenario.DISPATCHER);
        closeOrders.close(RUN_DATE, DEPOT);
        plans.publish(plans.create(RUN_DATE, DEPOT).id());
        SignedInUser.signOut();
    }

    @Test
    void shouldReturnTheArrivalOfAPlannedOrderInTheDocumentedShape() throws Exception {
        mvc.perform(get(DELIVERIES).header(HttpHeaders.AUTHORIZATION, store(STORE_1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].orderRef").value("PL-1"))
                .andExpect(jsonPath("$.items[0].status").value("PLANNED"))
                .andExpect(jsonPath("$.items[0].arrival.from").isNotEmpty())
                .andExpect(jsonPath("$.items[0].arrival.to").isNotEmpty())
                .andExpect(jsonPath("$.items[0].arrival.lateRisk").isNumber())
                .andExpect(jsonPath("$.items[0].arrival.vehicleId").value(PlanScenario.USED_VEHICLE))
                .andExpect(jsonPath("$.items[0].arrival.tripNo").value(1))
                .andExpect(jsonPath("$.items[0].arrival.changedReason").doesNotExist())
                .andExpect(jsonPath("$.items[0].deferral").doesNotExist());
    }

    @Test
    void shouldReturnTheDeferralOfAMovedOrderInTheDocumentedShape() throws Exception {
        mvc.perform(get(DELIVERIES).header(HttpHeaders.AUTHORIZATION, store(HEAVY_STORE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("MOVED"))
                .andExpect(jsonPath("$.items[0].arrival").doesNotExist())
                .andExpect(jsonPath("$.items[0].deferral.id").isNotEmpty())
                .andExpect(jsonPath("$.items[0].deferral.kind").value("UNAVOIDABLE"))
                .andExpect(jsonPath("$.items[0].deferral.rule").isNotEmpty())
                .andExpect(jsonPath("$.items[0].deferral.reason").isNotEmpty())
                .andExpect(jsonPath("$.items[0].deferral.newDate").value(PlanScenario.NEXT_DAY.toString()))
                .andExpect(jsonPath("$.items[0].deferral.needsDecision").value(false))
                .andExpect(jsonPath("$.items[0].deferral.splitOffered").value(true));
    }

    private String store(String outletId) throws Exception {
        return ApiSignIn.bearer(mvc, outletId + "@example.lk", PASSWORD);
    }
}
