package com.synapse.waypoint.store;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.DeliveryOutcome;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.order.support.SignedInUser;

@SpringBootTest
@Transactional
class StoreWorkflowTests {
    @Autowired StoreController store;
    @Autowired OrderRepository orders;
    @Autowired OrderService orderService;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @BeforeEach void setup() {
        OrderFixtures.insertOutlet(jdbc, "OUT991", "Fresh", "Testdistrict");
        OrderFixtures.insertOutlet(jdbc, "OUT992", "Fresh", "Otherdistrict");
        OrderFixtures.insertUser(jdbc, "usr-t-store", "STORE_MANAGER", "OUT991");
        SignedInUser.asStoreManager("usr-t-store", "OUT991");
    }
    @AfterEach void signOut() { SignedInUser.signOut(); }

    @Test void closedRunRejectsConfirmationAndEditing() {
        Order order = OrderFixtures.save(orders, "OUT991", OrderStatus.PREPARED);
        jdbc.update("INSERT INTO order_runs(run_date,depot,orders_closed_at) VALUES (?,?,now())",
                OrderFixtures.RUN_DATE, OrderFixtures.DEPOT);
        assertThatThrownBy(() -> store.confirm(order.getId())).isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo(ErrorCode.ORDERS_CLOSED));
        assertThatThrownBy(() -> store.edit(order.getId(), new StoreController.Units(12))).isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo(ErrorCode.ORDERS_CLOSED));
    }

    @Test void confirmingAnOrderAlertsTheStoreOnce() {
        Order order = OrderFixtures.save(orders, "OUT991", OrderStatus.PREPARED);
        store.confirm(order.getId());
        entityManager.flush();
        var notices = jdbc.queryForList("SELECT user_id, type, severity, link FROM notifications WHERE type = 'ORDER_CONFIRMED'");
        assertThat(notices).singleElement().satisfies(notice -> {
            assertThat(notice.get("user_id")).isEqualTo("usr-t-store");
            assertThat(notice.get("severity")).isEqualTo("INFO");
            assertThat(notice.get("link")).isEqualTo("/store/orders/" + order.getId());
        });
    }

    @Test void confirmedOrderCanBeEditedButPlannedCannotAndOtherOutletIsHidden() {
        Order mine = OrderFixtures.save(orders, "OUT991", OrderStatus.CONFIRMED);
        Order planned = OrderFixtures.save(orders, "OUT991", OrderStatus.PLANNED);
        Order other = OrderFixtures.save(orders, "OUT992", OrderStatus.PREPARED);
        assertThat(store.edit(mine.getId(), new StoreController.Units(12)).units()).isEqualTo(12);
        entityManager.flush();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE type = 'ORDER_CONFIRMED'", Integer.class))
                .isZero();
        assertThatThrownBy(() -> store.edit(planned.getId(), new StoreController.Units(12))).isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo(ErrorCode.INVALID_STATUS));
        assertThatThrownBy(() -> store.detail(other.getId())).isInstanceOf(NotFoundException.class);
    }

    @Test void deliveryOutcomeAlertsTheCorrectStore() {
        Order delivered = OrderFixtures.save(orders, "OUT991", OrderStatus.ON_THE_WAY);
        Order failed = OrderFixtures.save(orders, "OUT991", OrderStatus.ON_THE_WAY);
        orderService.recordOutcome(delivered.getId(), DeliveryOutcome.DELIVERED, delivered.getUnits());
        orderService.recordOutcome(failed.getId(), DeliveryOutcome.FAILED, 0);
        entityManager.flush();
        assertThat(jdbc.queryForMap("SELECT user_id, severity, link FROM notifications WHERE type = 'DELIVERY_COMPLETED'"))
                .containsEntry("user_id", "usr-t-store")
                .containsEntry("severity", "INFO")
                .containsEntry("link", "/store/deliveries/" + delivered.getId() + "/receipt");
        assertThat(jdbc.queryForMap("SELECT user_id, severity, link FROM notifications WHERE type = 'DELIVERY_PROBLEM'"))
                .containsEntry("user_id", "usr-t-store")
                .containsEntry("severity", "CRITICAL")
                .containsEntry("link", "/store/deliveries/" + failed.getId() + "/problem");
    }

    @Test void receiptRequiresDeliveredOrder() {
        Order prepared = OrderFixtures.save(orders, "OUT991", OrderStatus.PREPARED);
        assertThatThrownBy(() -> store.receipt(prepared.getId(), new StoreController.Receipt(10, "")))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo(ErrorCode.INVALID_STATUS));
    }

    @Test void receiptConfirmationIsRecordedAndAcknowledged() {
        Order delivered = OrderFixtures.save(orders, "OUT991", OrderStatus.DELIVERED);
        store.receipt(delivered.getId(), new StoreController.Receipt(delivered.getUnits(), ""));
        entityManager.flush();
        assertThat(jdbc.queryForMap("SELECT user_id, link FROM notifications WHERE type = 'RECEIPT_CONFIRMED'"))
                .containsEntry("user_id", "usr-t-store")
                .containsEntry("link", "/store/deliveries/" + delivered.getId() + "/receipt");
    }
}
