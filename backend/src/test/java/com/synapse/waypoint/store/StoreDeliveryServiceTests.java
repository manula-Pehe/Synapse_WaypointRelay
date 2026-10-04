package com.synapse.waypoint.store;

import static com.synapse.waypoint.planning.support.PlanScenario.DEPOT;
import static com.synapse.waypoint.planning.support.PlanScenario.HEAVY_STORE;
import static com.synapse.waypoint.planning.support.PlanScenario.RUN_DATE;
import static com.synapse.waypoint.planning.support.PlanScenario.STORE_1;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.entity.OrderSource;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.service.CloseOrdersService;
import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.order.support.SignedInUser;
import com.synapse.waypoint.planning.domain.DeferralKind;
import com.synapse.waypoint.planning.service.PlanService;
import com.synapse.waypoint.planning.support.PlanScenario;

/**
 * What a store sees for a run: plan data on its orders and the orders moved away from that run. The
 * scenario plans PL-1 and PL-2 and defers PL-3 (heavy store, UNAVOIDABLE) to the next day. Data is invented.
 */
@SpringBootTest
@Transactional
class StoreDeliveryServiceTests {

    @Autowired StoreDeliveryService deliveries;
    @Autowired PlanService plans;
    @Autowired CloseOrdersService closeOrders;
    @Autowired OrderRepository orders;
    @Autowired DemoClock clock;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
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
    void shouldShowNoPlanDataWhileThePlanIsOnlyADraft() {
        plans.create(RUN_DATE, DEPOT);
        asStore(STORE_1);

        assertThat(deliveries.forRun(RUN_DATE)).singleElement().satisfies(view -> {
            assertThat(view.arrival()).isNull();
            assertThat(view.deferral()).isNull();
        });
    }

    @Test
    void shouldShowTheArrivalWindowOfAPlannedOrder() {
        publish();
        asStore(STORE_1);

        assertThat(deliveries.forRun(RUN_DATE)).singleElement().satisfies(view -> {
            assertThat(view.orderRef()).isEqualTo("PL-1");
            assertThat(view.status()).isEqualTo(OrderStatus.PLANNED);
            assertThat(view.deferral()).isNull();
            assertThat(view.arrival().vehicleId()).isEqualTo(PlanScenario.USED_VEHICLE);
            assertThat(view.arrival().tripNo()).isEqualTo(1);
            assertThat(view.arrival().from()).isBefore(view.arrival().to());
            assertThat(view.arrival().lateRisk()).isNotNull();
            assertThat(view.arrival().changedReason()).isNull();
        });
    }

    @Test
    void shouldShowTheDeferralOfAMovedOrderOnItsNewDate() {
        publish();
        asStore(HEAVY_STORE);

        assertThat(deliveries.forRun(PlanScenario.NEXT_DAY)).singleElement().satisfies(view -> {
            assertThat(view.orderRef()).isEqualTo("PL-3");
            assertThat(view.status()).isEqualTo(OrderStatus.MOVED);
            assertThat(view.arrival()).isNull();
            assertThat(view.deferral().newDate()).isEqualTo(PlanScenario.NEXT_DAY);
        });
    }

    @Test
    void shouldStillListAnOrderOnTheRunItWasMovedAwayFrom() {
        publish();
        asStore(HEAVY_STORE);

        assertThat(deliveries.forRun(RUN_DATE)).singleElement().satisfies(view -> {
            assertThat(view.orderRef()).isEqualTo("PL-3");
            assertThat(view.status()).isEqualTo(OrderStatus.MOVED);
            assertThat(view.deferral().kind()).isEqualTo(DeferralKind.UNAVOIDABLE);
            assertThat(view.deferral().splitOffered()).isTrue();
            assertThat(view.deferral().needsDecision()).isFalse();
            assertThat(view.deferral().storeChoice()).isNull();
            assertThat(view.deferral().id()).isNotBlank();
            assertThat(view.deferral().rule()).isNotNull();
            assertThat(view.deferral().reason()).isNotBlank();
        });
    }

    @Test
    void shouldNotOfferASplitForADeferralThatWasChosen() {
        publish();
        entityManager.flush();
        jdbc.update("UPDATE deferrals SET kind = 'CHOSEN'");
        entityManager.clear();
        asStore(HEAVY_STORE);

        assertThat(deliveries.forRun(RUN_DATE)).singleElement()
                .satisfies(view -> assertThat(view.deferral().splitOffered()).isFalse());
    }

    @Test
    void shouldNotShowAnotherOutletsOrdersOrDeferrals() {
        publish();
        asStore(PlanScenario.IDLE_STORE);

        assertThat(deliveries.forRun(RUN_DATE)).isEmpty();
        assertThat(deliveries.forRun(PlanScenario.NEXT_DAY)).isEmpty();
    }

    @Test
    void shouldShowTheReceiptTheStoreConfirmed() {
        publish();
        jdbc.update("""
                INSERT INTO receipts (id, order_id, received_units, note, received_by, received_at)
                VALUES ('rcp-1', ?, 9, '', ?, now())""", PlanScenario.orderId("PL-1"),
                PlanScenario.storeUser(STORE_1));
        asStore(STORE_1);

        assertThat(deliveries.forRun(RUN_DATE)).singleElement()
                .satisfies(view -> assertThat(view.receipt().receivedUnits()).isEqualTo(9));
    }

    @Test
    void shouldDefaultToTheCurrentRunDateRatherThanToday() {
        String outlet = "OUT991";
        OrderFixtures.insertOutlet(jdbc, outlet, "Fresh", "Testdistrict");
        OrderFixtures.insertUser(jdbc, "usr-t-store", "STORE_MANAGER", outlet);
        LocalDate runDate = clock.runDate();
        orders.saveAndFlush(com.synapse.waypoint.core.order.entity.Order.create(OrderFixtures.newOrder(outlet,
                OrderStatus.CONFIRMED, TemperatureRequirement.AMBIENT, 10, OrderSource.SEED, runDate),
                OrderFixtures.CREATED_AT));
        SignedInUser.asStoreManager("usr-t-store", outlet);

        assertThat(deliveries.forRun(null)).hasSize(1);
        assertThat(deliveries.forRun(runDate.plusDays(40))).isEmpty();
    }

    private void publish() {
        plans.publish(plans.create(RUN_DATE, DEPOT).id());
    }

    private static void asStore(String outletId) {
        SignedInUser.asStoreManager(PlanScenario.storeUser(outletId), outletId);
    }
}
