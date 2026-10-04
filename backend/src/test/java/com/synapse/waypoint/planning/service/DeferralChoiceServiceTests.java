package com.synapse.waypoint.planning.service;

import static com.synapse.waypoint.planning.support.PlanScenario.DEPOT;
import static com.synapse.waypoint.planning.support.PlanScenario.HEAVY_STORE;
import static com.synapse.waypoint.planning.support.PlanScenario.RUN_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import java.time.Instant;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.OrderEventDto;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.service.CloseOrdersService;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.order.support.SignedInUser;
import com.synapse.waypoint.planning.domain.StoreChoice;
import com.synapse.waypoint.planning.dto.DeferralDto;
import com.synapse.waypoint.planning.repository.DeferralChoiceRepository;
import com.synapse.waypoint.planning.support.PlanScenario;

/**
 * A store answers the deferral of its order. The scenario defers PL-3 (too heavy for any vehicle, so
 * UNAVOIDABLE, 10 units) of the heavy store; the plan is published, so the order is MOVED. Data is invented.
 */
@SpringBootTest
@Transactional
class DeferralChoiceServiceTests {

    private static final String DEFERRED_ORDER = PlanScenario.orderId("PL-3");
    private static final String HEAVY_STORE_MANAGER = PlanScenario.storeUser(HEAVY_STORE);
    private static final int DEFERRED_UNITS = 10;

    @Autowired DeferralChoiceService choices;
    @Autowired PlanQueryService query;
    @Autowired PlanService plans;
    @Autowired OrderService orders;
    @Autowired CloseOrdersService closeOrders;
    @Autowired DemoClock clock;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired PasswordEncoder encoder;
    @MockitoSpyBean DeferralChoiceRepository choiceRepository;

    private String deferralId;

    @BeforeEach
    void publishAPlanThatDefersTheHeavyOrder() {
        PlanScenario.insert(jdbc, encoder);
        SignedInUser.asDispatcher(PlanScenario.DISPATCHER);
        closeOrders.close(RUN_DATE, DEPOT);
        plans.publish(plans.create(RUN_DATE, DEPOT).id());
        deferralId = query.deferralForOrder(DEFERRED_ORDER).orElseThrow().id();
        SignedInUser.asStoreManager(HEAVY_STORE_MANAGER, HEAVY_STORE);
    }

    @AfterEach
    void signOut() {
        SignedInUser.signOut();
    }

    @Test
    void shouldKeepTheOrderUnchangedAndRecordWhoChoseWhen() {
        Instant before = clock.now();

        DeferralDto result = choices.choose(deferralId, StoreChoice.KEEP, null);

        assertThat(result.storeChoice()).isEqualTo(StoreChoice.KEEP);
        assertThat(orders.get(DEFERRED_ORDER).status()).isEqualTo(OrderStatus.MOVED);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT choice, units, chosen_by, chosen_at FROM deferral_choices WHERE deferral_id = ?", deferralId);
        assertThat(row).containsEntry("choice", "KEEP").containsEntry("units", null)
                .containsEntry("chosen_by", HEAVY_STORE_MANAGER);
        assertThat(((java.sql.Timestamp) row.get("chosen_at")).toInstant()).isBetween(before, clock.now());
    }

    @Test
    void shouldCancelTheOrderAndWriteHistory() {
        DeferralDto result = choices.choose(deferralId, StoreChoice.CANCEL, null);

        assertThat(result.storeChoice()).isEqualTo(StoreChoice.CANCEL);
        assertThat(orders.get(DEFERRED_ORDER).status()).isEqualTo(OrderStatus.CANCELLED);
        OrderEventDto event = lastEvent();
        assertThat(event.type()).isEqualTo("CANCELLED");
        assertThat(event.fromStatus()).isEqualTo(OrderStatus.MOVED);
        assertThat(event.details()).containsEntry("reason", "Store cancelled after deferral");
    }

    @Test
    void shouldReduceTheOrderAndWriteHistory() {
        DeferralDto result = choices.choose(deferralId, StoreChoice.REDUCE, 4);

        assertThat(result.storeChoice()).isEqualTo(StoreChoice.REDUCE);
        OrderDto order = orders.get(DEFERRED_ORDER);
        assertThat(order.units()).isEqualTo(4);
        assertThat(order.status()).isEqualTo(OrderStatus.MOVED);
        assertThat(lastEvent().type()).isEqualTo("EDITED");
        assertThat(storedUnits()).isEqualTo(4);
    }

    @Test
    void shouldRecordASplitRequestWithoutChangingTheOrderWhenItIsOffered() {
        DeferralDto result = choices.choose(deferralId, StoreChoice.SPLIT, null);

        assertThat(result.storeChoice()).isEqualTo(StoreChoice.SPLIT);
        OrderDto order = orders.get(DEFERRED_ORDER);
        assertThat(order.units()).isEqualTo(DEFERRED_UNITS);
        assertThat(order.status()).isEqualTo(OrderStatus.MOVED);
    }

    @Test
    void shouldRefuseASplitWhenTheDeferralWasChosenRatherThanUnavoidable() {
        updateRow("UPDATE deferrals SET kind = 'CHOSEN' WHERE id = ?", deferralId);

        assertRejected(ErrorCode.VALIDATION, () -> choices.choose(deferralId, StoreChoice.SPLIT, null));
        assertThat(storedChoices()).isZero();
    }

    @Test
    void shouldRefuseAReduceOutsideOneToLessThanTheCurrentUnits() {
        for (Integer units : new Integer[] { null, 0, -2, DEFERRED_UNITS, DEFERRED_UNITS + 5 }) {
            assertRejected(ErrorCode.VALIDATION, () -> choices.choose(deferralId, StoreChoice.REDUCE, units));
        }
        assertThat(storedChoices()).isZero();
        assertThat(orders.get(DEFERRED_ORDER).units()).isEqualTo(DEFERRED_UNITS);
    }

    @Test
    void shouldHideTheDeferralFromAnotherOutletsManager() {
        SignedInUser.asStoreManager(PlanScenario.storeUser(PlanScenario.STORE_1), PlanScenario.STORE_1);

        assertRejected(ErrorCode.NOT_FOUND, () -> choices.choose(deferralId, StoreChoice.CANCEL, null));
        assertThat(jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, DEFERRED_ORDER))
                .isEqualTo("MOVED");
        assertThat(storedChoices()).isZero();
    }

    @Test
    void shouldRefuseAnUnknownDeferral() {
        assertRejected(ErrorCode.NOT_FOUND, () -> choices.choose("dfr-missing", StoreChoice.KEEP, null));
    }

    @Test
    void shouldRefuseAChoiceWhenTheOrderIsNoLongerMoved() {
        updateRow("UPDATE orders SET status = 'CONFIRMED' WHERE id = ?", DEFERRED_ORDER);

        assertRejected(ErrorCode.INVALID_STATUS, () -> choices.choose(deferralId, StoreChoice.KEEP, null));
        assertThat(storedChoices()).isZero();
    }

    @Test
    void shouldRefuseASecondChoiceWhateverTheFirstOneDid() {
        choices.choose(deferralId, StoreChoice.CANCEL, null);

        assertRejected(ErrorCode.DUPLICATE, () -> choices.choose(deferralId, StoreChoice.KEEP, null));
        assertThat(storedChoices()).isEqualTo(1);
    }

    @Test
    void shouldAnswerDuplicateWithoutBreakingTheTransactionWhenTwoRequestsRace() {
        choices.choose(deferralId, StoreChoice.KEEP, null);
        doReturn(false).when(choiceRepository).existsByDeferralId(anyString());

        assertRejected(ErrorCode.DUPLICATE, () -> choices.choose(deferralId, StoreChoice.KEEP, null));

        assertThat(storedChoices()).isEqualTo(1);
    }

    @Test
    void shouldTellTheDispatchersOfTheOutletsDepotAndNoOneElse() {
        choices.choose(deferralId, StoreChoice.CANCEL, null);
        entityManager.flush();

        List<String> notified = jdbc.queryForList(
                "SELECT user_id FROM notifications WHERE type = 'STORE_DEFERRAL_CHOICE'", String.class);
        assertThat(notified).containsExactlyInAnyOrder(PlanScenario.DISPATCHER, PlanScenario.DEPOT_DISPATCHER);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT severity, title, link FROM notifications WHERE user_id = ?", PlanScenario.DEPOT_DISPATCHER);
        assertThat(row).containsEntry("severity", "INFO").containsEntry("title", "Store chose CANCEL for PL-3")
                .containsEntry("link", "/dispatch");
    }

    @Test
    void shouldNotNotifyWhenTheChoiceIsRefused() {
        assertRejected(ErrorCode.VALIDATION, () -> choices.choose(deferralId, StoreChoice.REDUCE, null));
        entityManager.flush();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE type = 'STORE_DEFERRAL_CHOICE'",
                Integer.class)).isZero();
    }

    private void assertRejected(ErrorCode code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(DomainException.class,
                error -> assertThat(error.code()).isEqualTo(code));
    }

    private OrderEventDto lastEvent() {
        List<OrderEventDto> history = orders.history(DEFERRED_ORDER);
        return history.get(history.size() - 1);
    }

    private int storedChoices() {
        return jdbc.queryForObject("SELECT count(*) FROM deferral_choices WHERE deferral_id = ?", Integer.class,
                deferralId);
    }

    private int storedUnits() {
        return jdbc.queryForObject("SELECT units FROM orders WHERE id = ?", Integer.class, DEFERRED_ORDER);
    }

    /** Changes a row behind JPA's back, so the cached entities are dropped to make the service re-read it. */
    private void updateRow(String sql, Object... args) {
        entityManager.flush();
        jdbc.update(sql, args);
        entityManager.clear();
    }
}
