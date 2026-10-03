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

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.order.support.SignedInUser;

@SpringBootTest
@Transactional
class StoreWorkflowTests {
    @Autowired StoreController store;
    @Autowired OrderRepository orders;
    @Autowired JdbcTemplate jdbc;

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

    @Test void nonPreparedOrderCannotBeEditedAndOtherOutletIsHidden() {
        Order mine = OrderFixtures.save(orders, "OUT991", OrderStatus.CONFIRMED);
        Order other = OrderFixtures.save(orders, "OUT992", OrderStatus.PREPARED);
        assertThatThrownBy(() -> store.edit(mine.getId(), new StoreController.Units(12))).isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo(ErrorCode.INVALID_STATUS));
        assertThatThrownBy(() -> store.detail(other.getId())).isInstanceOf(NotFoundException.class);
    }

    @Test void receiptRequiresDeliveredOrder() {
        Order prepared = OrderFixtures.save(orders, "OUT991", OrderStatus.PREPARED);
        assertThatThrownBy(() -> store.receipt(prepared.getId(), new StoreController.Receipt(10, "")))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo(ErrorCode.INVALID_STATUS));
    }
}
