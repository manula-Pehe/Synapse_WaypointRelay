package com.synapse.waypoint.core.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.core.order.dto.CreateOrderRequest;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.order.support.SignedInUser;

/** What store managers and the dispatcher may still do once the orders of a run are closed. */
@SpringBootTest
@Transactional
class OrderChangesAfterCloseTests {

    private static final String OUTLET = "OUT991";
    private static final String OTHER_DEPOT_OUTLET = "OUT993";
    private static final String STORE_USER = "usr-t-store";
    private static final String DISPATCHER = "usr-t-dispatch";

    private interface StoreChange {
        void apply(OrderService service, String orderId);
    }

    @Autowired OrderService service;
    @Autowired CloseOrdersService closeOrders;
    @Autowired OrderRepository orders;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void createOutletsAndUsers() {
        OrderFixtures.insertOutlet(jdbc, OUTLET, "Fresh", "Testdistrict");
        OrderFixtures.insertOutlet(jdbc, OTHER_DEPOT_OUTLET, "Fresh", "Otherdistrict", "Otherdepot");
        OrderFixtures.insertUser(jdbc, STORE_USER, "STORE_MANAGER", OUTLET);
        OrderFixtures.insertUser(jdbc, DISPATCHER, "DISPATCHER", null);
    }

    @AfterEach
    void signOut() {
        SignedInUser.signOut();
    }

    static Stream<Arguments> storeChanges() {
        return Stream.of(
                Arguments.of("confirm", (StoreChange) (s, id) -> s.confirm(id)),
                Arguments.of("editUnits", (StoreChange) (s, id) -> s.editUnits(id, 5)),
                Arguments.of("cancel", (StoreChange) (s, id) -> s.cancel(id, "Not needed")));
    }

    @ParameterizedTest(name = "store {0} is refused after close")
    @MethodSource("storeChanges")
    void shouldRefuseStoreChangesAfterTheOrdersAreClosed(String name, StoreChange change) {
        Order order = prepared(OUTLET);
        closeOrders.close(OrderFixtures.RUN_DATE, OrderFixtures.DEPOT);
        SignedInUser.asStoreManager(STORE_USER, OUTLET);

        assertThatThrownBy(() -> change.apply(service, order.getId())).isInstanceOfSatisfying(DomainException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.ORDERS_CLOSED));
        assertThat(service.get(order.getId()).units()).isEqualTo(10);
    }

    @ParameterizedTest(name = "store {0} works before close")
    @MethodSource("storeChanges")
    void shouldAllowStoreChangesBeforeTheOrdersAreClosed(String name, StoreChange change) {
        Order order = prepared(OUTLET);
        SignedInUser.asStoreManager(STORE_USER, OUTLET);

        change.apply(service, order.getId());

        assertThat(service.history(order.getId())).hasSize(1);
    }

    @Test
    void shouldStillLetTheDispatcherChangeOrdersAfterClose() {
        Order order = prepared(OUTLET);
        closeOrders.close(OrderFixtures.RUN_DATE, OrderFixtures.DEPOT);
        SignedInUser.asDispatcher(DISPATCHER);

        OrderDto cancelled = service.cancel(order.getId(), "Store closed for the day");

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldNotBlockAStoreWhoseDepotRunIsStillOpen() {
        Order order = prepared(OUTLET);
        closeOrders.close(OrderFixtures.RUN_DATE, "Otherdepot");
        SignedInUser.asStoreManager(STORE_USER, OUTLET);

        assertThat(service.confirm(order.getId()).status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void shouldRefuseANewStoreOrderForAClosedRun() {
        closeOrders.close(OrderFixtures.RUN_DATE, OrderFixtures.DEPOT);
        SignedInUser.asStoreManager(STORE_USER, OUTLET);

        assertThatThrownBy(() -> service.createStoreOrder(request(OUTLET, OrderFixtures.RUN_DATE)))
                .isInstanceOfSatisfying(DomainException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.ORDERS_CLOSED));
    }

    @Test
    void shouldAllowANewStoreOrderForALaterRun() {
        closeOrders.close(OrderFixtures.RUN_DATE, OrderFixtures.DEPOT);
        SignedInUser.asStoreManager(STORE_USER, OUTLET);

        OrderDto order = service.createStoreOrder(request(OUTLET, OrderFixtures.RUN_DATE.plusDays(1)));

        assertThat(order.status()).isEqualTo(OrderStatus.PREPARED);
    }

    @Test
    void shouldRefuseAPhoneInOrderForAClosedRun() {
        closeOrders.close(OrderFixtures.RUN_DATE, OrderFixtures.DEPOT);
        SignedInUser.asDispatcher(DISPATCHER);

        assertThatThrownBy(() -> service.createPhoneInOrder(request(OUTLET, OrderFixtures.RUN_DATE)))
                .isInstanceOfSatisfying(DomainException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.ORDERS_CLOSED));
    }

    @Test
    void shouldAllowAPhoneInOrderForALaterRunOrAnotherDepot() {
        closeOrders.close(OrderFixtures.RUN_DATE, OrderFixtures.DEPOT);
        SignedInUser.asDispatcher(DISPATCHER);

        assertThat(service.createPhoneInOrder(request(OUTLET, OrderFixtures.RUN_DATE.plusDays(1))).status())
                .isEqualTo(OrderStatus.CONFIRMED);
        assertThat(service.createPhoneInOrder(request(OTHER_DEPOT_OUTLET, OrderFixtures.RUN_DATE)).status())
                .isEqualTo(OrderStatus.CONFIRMED);
    }

    private Order prepared(String outletId) {
        return OrderFixtures.save(orders, outletId, "Style", TemperatureRequirement.CHILLED, OrderStatus.PREPARED);
    }

    private static CreateOrderRequest request(String outletId, LocalDate runDate) {
        return new CreateOrderRequest(outletId, runDate, TemperatureRequirement.AMBIENT, 6, null);
    }
}
