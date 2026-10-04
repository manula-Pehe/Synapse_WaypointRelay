package com.synapse.waypoint.planning.service;

import static com.synapse.waypoint.planning.support.PlanScenario.DEPOT;
import static com.synapse.waypoint.planning.support.PlanScenario.NEXT_DAY;
import static com.synapse.waypoint.planning.support.PlanScenario.RUN_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.service.CloseOrdersService;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.order.support.SignedInUser;
import com.synapse.waypoint.planning.domain.DeferralKind;
import com.synapse.waypoint.planning.domain.PlanStatus;
import com.synapse.waypoint.planning.dto.DeferralDto;
import com.synapse.waypoint.planning.dto.PlanDto;
import com.synapse.waypoint.planning.dto.PlanTripDto;
import com.synapse.waypoint.planning.support.PlanScenario;

/** Creating, reading and publishing plans against the real services. Data is invented. */
@SpringBootTest
@Transactional
class PlanServiceTests {

    @Autowired PlanService plans;
    @Autowired OrderService orders;
    @Autowired CloseOrdersService closeOrders;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManager entityManager;

    @BeforeEach
    void createDepotDay() {
        PlanScenario.insert(jdbc, encoder);
        SignedInUser.asDispatcher(PlanScenario.DISPATCHER);
    }

    @AfterEach
    void signOut() {
        SignedInUser.signOut();
    }

    @Test
    void shouldRefuseToCreateAPlanBeforeOrdersAreClosed() {
        assertThatThrownBy(() -> plans.create(RUN_DATE, DEPOT))
                .isInstanceOfSatisfying(DomainException.class,
                        error -> assertThat(error.code()).isEqualTo(ErrorCode.ORDERS_NOT_CLOSED));
    }

    @Test
    void shouldCreateADraftWithTripsStopsAndDeferralsCreatedByTheDispatcher() {
        closeOrders.close(RUN_DATE, DEPOT);

        PlanDto plan = plans.create(RUN_DATE, DEPOT);

        assertThat(plan).extracting(PlanDto::runDate, PlanDto::depot, PlanDto::version, PlanDto::status)
                .containsExactly(RUN_DATE, DEPOT, 1, PlanStatus.DRAFT);
        assertThat(plan.summary()).extracting("served", "deferred", "unavoidable", "chosen", "violations")
                .containsExactly(2, 1, 1, 0, 0);
        assertThat(plan.vehicles()).singleElement().satisfies(vehicle -> {
            assertThat(vehicle.vehicleId()).isEqualTo(PlanScenario.USED_VEHICLE);
            assertThat(vehicle.freshBudget()).isEqualTo(270);
            assertThat(vehicle.freshMinutesUsed()).isPositive();
        });
        PlanTripDto trip = plan.vehicles().get(0).trips().get(0);
        assertThat(trip.departAt().toString()).isEqualTo("2030-01-10T03:30+05:30");
        assertThat(trip.stops()).extracting("orderRef", "seq", "loadSeq", "temp")
                .containsExactly(tuple("PL-1", 1, 2, "CHILLED"), tuple("PL-2", 2, 1, "CHILLED"));
        entityManager.flush();
        assertThat(jdbc.queryForObject("SELECT created_by FROM plans WHERE id = ?", String.class, plan.id()))
                .isEqualTo(PlanScenario.DISPATCHER);
    }

    @Test
    void shouldExplainTheDeferralOfAnOrderNoVehicleCanCarry() {
        closeOrders.close(RUN_DATE, DEPOT);
        PlanDto plan = plans.create(RUN_DATE, DEPOT);

        List<DeferralDto> deferrals = plans.deferrals(plan.id());

        assertThat(deferrals).singleElement().satisfies(deferral -> {
            assertThat(deferral.orderRef()).isEqualTo("PL-3");
            assertThat(deferral.outletId()).isEqualTo(PlanScenario.HEAVY_STORE);
            assertThat(deferral.kind()).isEqualTo(DeferralKind.UNAVOIDABLE);
            assertThat(deferral.reason()).isNotBlank();
            assertThat(deferral.newDate()).isEqualTo(NEXT_DAY);
            assertThat(deferral.storeChoice()).isNull();
        });
    }

    @Test
    void shouldReplaceAnExistingDraftAsVersionOne() {
        closeOrders.close(RUN_DATE, DEPOT);
        PlanDto first = plans.create(RUN_DATE, DEPOT);

        PlanDto second = plans.create(RUN_DATE, DEPOT);

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(second.version()).isEqualTo(1);
        entityManager.flush();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plans WHERE run_date = ? AND depot = ?", Integer.class,
                RUN_DATE, DEPOT)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stops WHERE trip_id IN (SELECT id FROM trips)",
                Integer.class)).isGreaterThanOrEqualTo(2);
        assertThatThrownBy(() -> plans.get(first.id())).isInstanceOf(NotFoundException.class);
        assertThat(plans.latest(RUN_DATE, DEPOT).id()).isEqualTo(second.id());
    }

    @Test
    void shouldRefuseToCreateAfterThePlanIsPublished() {
        closeOrders.close(RUN_DATE, DEPOT);
        plans.publish(plans.create(RUN_DATE, DEPOT).id());

        assertThatThrownBy(() -> plans.create(RUN_DATE, DEPOT))
                .isInstanceOfSatisfying(DomainException.class,
                        error -> assertThat(error.code()).isEqualTo(ErrorCode.PLAN_LOCKED));
    }

    @Test
    void shouldAnswerNotFoundWhenNoPlanExists() {
        assertThatThrownBy(() -> plans.latest(RUN_DATE, DEPOT)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> plans.get("pln-missing")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldMovePlacedOrdersToPlannedAndDeferredOrdersToMovedWithHistory() {
        closeOrders.close(RUN_DATE, DEPOT);
        PlanDto draft = plans.create(RUN_DATE, DEPOT);

        PlanDto published = plans.publish(draft.id());

        assertThat(published.status()).isEqualTo(PlanStatus.PUBLISHED);
        entityManager.flush();
        assertThat(jdbc.queryForMap("SELECT published_by FROM plans WHERE id = ?", draft.id()))
                .containsEntry("published_by", PlanScenario.DISPATCHER);
        OrderDto placed = orders.get(PlanScenario.orderId("PL-1"));
        OrderDto deferred = orders.get(PlanScenario.orderId("PL-3"));
        assertThat(placed.status()).isEqualTo(OrderStatus.PLANNED);
        assertThat(deferred.status()).isEqualTo(OrderStatus.MOVED);
        assertThat(deferred.runDate()).isEqualTo(NEXT_DAY);
        assertThat(orders.history(placed.id())).extracting("toStatus").contains(OrderStatus.PLANNED);
        assertThat(orders.history(deferred.id())).extracting("toStatus").contains(OrderStatus.MOVED);
        assertThat(orders.history(placed.id()).get(orders.history(placed.id()).size() - 1).details())
                .containsEntry("planId", draft.id());
    }

    @Test
    void shouldRefuseToPublishTheSamePlanTwice() {
        closeOrders.close(RUN_DATE, DEPOT);
        String planId = plans.create(RUN_DATE, DEPOT).id();
        plans.publish(planId);
        long notificationsAfterFirst = notificationCount();

        assertThatThrownBy(() -> plans.publish(planId))
                .isInstanceOfSatisfying(DomainException.class,
                        error -> assertThat(error.code()).isEqualTo(ErrorCode.PLAN_LOCKED));
        assertThat(notificationCount()).isEqualTo(notificationsAfterFirst);
    }

    @Test
    void shouldNotifyOnlyThePeopleTheOwnPlanAffects() {
        closeOrders.close(RUN_DATE, DEPOT);
        long before = notificationCount();

        plans.publish(plans.create(RUN_DATE, DEPOT).id());

        entityManager.flush();
        List<Map<String, Object>> sent = jdbc.queryForList(
                "SELECT user_id, severity, type, title, body, link FROM notifications ORDER BY user_id");
        assertThat(sent).hasSize((int) before + 5);
        assertThat(sent).extracting(row -> row.get("user_id"))
                .containsExactlyInAnyOrder(PlanScenario.storeUser(PlanScenario.STORE_1),
                        PlanScenario.storeUser(PlanScenario.STORE_2), PlanScenario.storeUser(PlanScenario.HEAVY_STORE),
                        PlanScenario.LOADER, PlanScenario.USED_DRIVER);
        assertThat(rowFor(sent, PlanScenario.storeUser(PlanScenario.STORE_1)))
                .containsEntry("severity", "INFO").containsEntry("title", "Delivery scheduled for Thu 10 Jan")
                .containsEntry("link", "/store/deliveries");
        assertThat((String) rowFor(sent, PlanScenario.storeUser(PlanScenario.STORE_1)).get("body"))
                .contains("Expected arrival:").contains("–");
        assertThat(rowFor(sent, PlanScenario.storeUser(PlanScenario.HEAVY_STORE)))
                .containsEntry("severity", "CRITICAL").containsEntry("title", "Order PL-3 deferred to Fri 11 Jan")
                .containsEntry("link", "/store/orders/" + PlanScenario.orderId("PL-3"));
        assertThat(rowFor(sent, PlanScenario.LOADER)).containsEntry("title", "Loading lists ready")
                .containsEntry("link", "/loader");
        assertThat(rowFor(sent, PlanScenario.USED_DRIVER)).containsEntry("title", "Your trips are ready")
                .containsEntry("link", "/driver");
    }

    private static Map<String, Object> rowFor(List<Map<String, Object>> sent, String userId) {
        return sent.stream().filter(row -> userId.equals(row.get("user_id"))).findFirst().orElseThrow();
    }

    private long notificationCount() {
        entityManager.flush();
        return jdbc.queryForObject("SELECT count(*) FROM notifications", Long.class);
    }
}
