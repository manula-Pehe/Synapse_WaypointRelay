package com.synapse.waypoint.planning.service;

import static com.synapse.waypoint.planning.support.PlanScenario.DEPOT;
import static com.synapse.waypoint.planning.support.PlanScenario.RUN_DATE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.service.CloseOrdersService;
import com.synapse.waypoint.core.order.support.SignedInUser;
import com.synapse.waypoint.planning.domain.DeferralKind;
import com.synapse.waypoint.planning.dto.PlanTripDto;
import com.synapse.waypoint.planning.support.PlanScenario;

/**
 * What other modules read from the published plan, called as the roles that really call it. Data is
 * invented.
 */
@SpringBootTest
@Transactional
class PlanQueryServiceTests {

    private static final String OTHER_STORES_ORDER = PlanScenario.orderId("PL-2");
    private static final String DEFERRED_ORDER = PlanScenario.orderId("PL-3");

    @Autowired PlanQueryService query;
    @Autowired PlanService plans;
    @Autowired CloseOrdersService closeOrders;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void createDepotDay() {
        PlanScenario.insert(jdbc, encoder);
        SignedInUser.asDispatcher(PlanScenario.DISPATCHER);
        closeOrders.close(RUN_DATE, DEPOT);
    }

    @AfterEach
    void signOut() {
        SignedInUser.signOut();
    }

    @Test
    void shouldReturnNothingWhileThePlanIsOnlyADraft() {
        String draftId = plans.create(RUN_DATE, DEPOT).id();
        String stopOrder = PlanScenario.orderId("PL-1");

        assertThat(query.publishedPlan(RUN_DATE, DEPOT)).isEmpty();
        assertThat(query.tripsForDepot(RUN_DATE, DEPOT)).isEmpty();
        assertThat(query.tripsForVehicle(RUN_DATE, PlanScenario.USED_VEHICLE)).isEmpty();
        assertThat(query.stopForOrder(stopOrder)).isEmpty();
        assertThat(query.deferralForOrder(DEFERRED_ORDER)).isEmpty();
        assertThat(plans.get(draftId).status().name()).isEqualTo("DRAFT");
    }

    @Test
    void shouldReturnThePublishedPlansDataToTheDriverOfTheVehicle() {
        String planId = publish();
        SignedInUser.asDriver(PlanScenario.USED_DRIVER, PlanScenario.USED_VEHICLE);

        List<PlanTripDto> trips = query.tripsForVehicle(RUN_DATE, PlanScenario.USED_VEHICLE);

        assertThat(trips).singleElement().satisfies(trip -> assertThat(trip.stops()).extracting("orderRef")
                .containsExactly("PL-1", "PL-2"));
        assertThat(query.publishedPlan(RUN_DATE, DEPOT)).get().extracting("id").isEqualTo(planId);
        assertThat(query.tripsForVehicle(RUN_DATE, PlanScenario.IDLE_VEHICLE)).isEmpty();
    }

    @Test
    void shouldShowAStoreManagerTheStopsAndDeferralsOfOtherStoresToo() {
        publish();
        SignedInUser.asStoreManager(PlanScenario.storeUser(PlanScenario.STORE_1), PlanScenario.STORE_1);

        assertThat(query.stopForOrder(OTHER_STORES_ORDER)).get().satisfies(placement -> {
            assertThat(placement.vehicleId()).isEqualTo(PlanScenario.USED_VEHICLE);
            assertThat(placement.stop().orderRef()).isEqualTo("PL-2");
            assertThat(placement.stop().outletId()).isEqualTo(PlanScenario.STORE_2);
        });
        assertThat(query.deferralForOrder(DEFERRED_ORDER)).get().satisfies(deferral -> {
            assertThat(deferral.orderRef()).isEqualTo("PL-3");
            assertThat(deferral.kind()).isEqualTo(DeferralKind.UNAVOIDABLE);
        });
        assertThat(query.tripsForDepot(RUN_DATE, DEPOT)).singleElement()
                .satisfies(trip -> assertThat(trip.stops()).hasSize(2));
    }

    @Test
    void shouldServeALoaderTheDepotsTripsAndNothingForAnOrderThePlanDoesNotPlace() {
        publish();
        SignedInUser.asLoader(PlanScenario.LOADER, DEPOT);

        assertThat(query.tripsForDepot(RUN_DATE, DEPOT.toUpperCase())).hasSize(1);
        assertThat(query.tripsForDepot(RUN_DATE, PlanScenario.OTHER_DEPOT)).isEmpty();
        assertThat(query.stopForOrder(DEFERRED_ORDER)).isEmpty();
        assertThat(query.deferralForOrder(PlanScenario.orderId("PL-1"))).isEmpty();
    }

    private String publish() {
        return plans.publish(plans.create(RUN_DATE, DEPOT).id()).id();
    }
}
