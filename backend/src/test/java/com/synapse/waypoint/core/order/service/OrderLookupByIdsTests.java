package com.synapse.waypoint.core.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.order.support.SignedInUser;

/** The by-id lookup used by read facades: every role gets every requested order. Data is invented. */
@SpringBootTest
@Transactional
class OrderLookupByIdsTests {

    private static final String OUTLET = "OUT991";
    private static final String OTHER_OUTLET = "OUT992";

    @Autowired OrderService service;
    @Autowired OrderRepository orders;
    @Autowired JdbcTemplate jdbc;

    private Order own;
    private Order others;

    @BeforeEach
    void createOrders() {
        OrderFixtures.insertOutlet(jdbc, OUTLET, "Fresh", "Testdistrict");
        OrderFixtures.insertOutlet(jdbc, OTHER_OUTLET, "Fresh", "Otherdistrict");
        OrderFixtures.insertUser(jdbc, "usr-t-store", "STORE_MANAGER", OUTLET);
        own = OrderFixtures.save(orders, OUTLET, OrderStatus.CONFIRMED);
        others = OrderFixtures.save(orders, OTHER_OUTLET, OrderStatus.CONFIRMED);
    }

    @AfterEach
    void signOut() {
        SignedInUser.signOut();
    }

    @Test
    void shouldReturnOrdersOfOtherOutletsToAStoreManager() {
        SignedInUser.asStoreManager("usr-t-store", OUTLET);

        List<OrderDto> found = service.findByIds(List.of(own.getId(), others.getId()));

        assertThat(found).extracting(OrderDto::id).containsExactlyInAnyOrder(own.getId(), others.getId());
    }

    @Test
    void shouldLeaveOutUnknownIdsAndAnswerEmptyForNoIds() {
        assertThat(service.findByIds(List.of(own.getId(), "missing"))).extracting(OrderDto::id)
                .containsExactly(own.getId());
        assertThat(service.findByIds(List.of())).isEmpty();
    }
}
