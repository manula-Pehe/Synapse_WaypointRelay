package com.synapse.waypoint.store;

import static com.synapse.waypoint.planning.support.PlanScenario.DEPOT;
import static com.synapse.waypoint.planning.support.PlanScenario.PASSWORD;
import static com.synapse.waypoint.planning.support.PlanScenario.RUN_DATE;
import static com.synapse.waypoint.planning.support.PlanScenario.STORE_1;
import static com.synapse.waypoint.planning.support.PlanScenario.STORE_2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.service.CloseOrdersService;
import com.synapse.waypoint.core.order.support.ApiSignIn;
import com.synapse.waypoint.core.order.support.SignedInUser;
import com.synapse.waypoint.planning.service.PlanService;
import com.synapse.waypoint.planning.support.PlanScenario;

/** POST /api/store/failed/{deliveryId}/choice (S3f) over HTTP. Data is invented. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StoreFailedChoiceApiTests {

    private static final String DELIVERY_ID = "dlv-failed-1";
    private static final String ORDER_REF = "PL-1";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired CloseOrdersService closeOrders;
    @Autowired PlanService plans;
    @Autowired EntityManager entityManager;

    @BeforeEach
    void publishAPlanAndFailTheFirstDelivery() {
        PlanScenario.insert(jdbc, encoder);
        SignedInUser.asDispatcher(PlanScenario.DISPATCHER);
        closeOrders.close(RUN_DATE, DEPOT);
        plans.publish(plans.create(RUN_DATE, DEPOT).id());
        SignedInUser.signOut();
        insertDelivery(DELIVERY_ID, ORDER_REF, "FAILED");
    }

    @ParameterizedTest
    @ValueSource(strings = { "REPLAN_TOMORROW", "TRY_LATER_TODAY", "CANCEL" })
    void shouldRecordEachChoiceOnTheDelivery(String choice) throws Exception {
        choose(STORE_1, DELIVERY_ID, choice).andExpect(status().isNoContent());
        entityManager.flush();

        assertThat(jdbc.queryForObject("SELECT decision FROM deliveries WHERE id = ?", String.class, DELIVERY_ID))
                .isEqualTo(choice);
    }

    @Test
    void shouldTellTheDepotDispatchersWhatTheStoreChose() throws Exception {
        choose(STORE_1, DELIVERY_ID, "TRY_LATER_TODAY").andExpect(status().isNoContent());
        entityManager.flush();

        assertThat(jdbc.queryForMap("""
                SELECT severity, link, body FROM notifications
                WHERE type = 'STORE_FAILED_CHOICE' AND user_id = ?""", PlanScenario.DEPOT_DISPATCHER))
                .containsEntry("severity", "INFO")
                .containsEntry("link", "/dispatch")
                .containsEntry("body", "Store chose TRY_LATER_TODAY for failed " + ORDER_REF);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notifications WHERE type = 'STORE_FAILED_CHOICE' AND user_id = ?""",
                Integer.class, PlanScenario.OTHER_DEPOT_DISPATCHER)).isZero();
    }

    @Test
    void shouldHideAnotherOutletsDelivery() throws Exception {
        choose(STORE_2, DELIVERY_ID, "CANCEL")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldRefuseADeliveryThatDidNotFail() throws Exception {
        insertDelivery("dlv-partial-1", "PL-2", "PARTIAL");

        choose(STORE_2, "dlv-partial-1", "CANCEL")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS"));
    }

    @Test
    void shouldRefuseASecondAnswer() throws Exception {
        choose(STORE_1, DELIVERY_ID, "REPLAN_TOMORROW").andExpect(status().isNoContent());

        choose(STORE_1, DELIVERY_ID, "CANCEL")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
    }

    @Test
    void shouldRefuseAChoiceThatIsNotOffered() throws Exception {
        choose(STORE_1, DELIVERY_ID, "SHRUG").andExpect(status().isBadRequest());
    }

    @Test
    void shouldExposeTheDeliveryIdToTheStore() throws Exception {
        mvc.perform(get("/api/store/deliveries?runDate=" + RUN_DATE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(STORE_1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].delivery.id").value(DELIVERY_ID))
                .andExpect(jsonPath("$.items[0].delivery.outcome").value("FAILED"));
    }

    private ResultActions choose(String outletId, String deliveryId, String choice) throws Exception {
        return mvc.perform(post("/api/store/failed/" + deliveryId + "/choice")
                .header(HttpHeaders.AUTHORIZATION, bearer(outletId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"choice\":\"" + choice + "\"}"));
    }

    private String bearer(String outletId) throws Exception {
        return ApiSignIn.bearer(mvc, outletId + "@example.lk", PASSWORD);
    }

    private void insertDelivery(String deliveryId, String orderRef, String outcome) {
        String orderId = PlanScenario.orderId(orderRef);
        jdbc.update("""
                INSERT INTO deliveries (id, stop_id, order_id, vehicle_id, outcome, units, reason, arrived_at,
                                        completed_at, client_id)
                VALUES (?, (SELECT id FROM stops WHERE order_id = ?), ?, ?, ?, 0, 'STORE_CLOSED', now(), now(), ?)""",
                deliveryId, orderId, orderId, PlanScenario.USED_VEHICLE, outcome, "client-" + deliveryId);
    }
}
