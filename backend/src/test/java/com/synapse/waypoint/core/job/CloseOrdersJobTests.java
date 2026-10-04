package com.synapse.waypoint.core.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.service.CloseOrdersService;
import com.synapse.waypoint.core.order.support.OrderFixtures;

/** The 4 PM automatic close. Outlets and orders are invented. */
@SpringBootTest
@Transactional
class CloseOrdersJobTests {

    private static final JobRun RUN = new JobRun(OrderFixtures.RUN_DATE, OrderFixtures.DEPOT);
    private static final String OUTLET = "OUT951";

    @Autowired CloseOrdersJob job;
    @Autowired CloseOrdersService closeOrders;
    @Autowired OrderRepository orders;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void createOutlet() {
        OrderFixtures.insertOutlet(jdbc, OUTLET, "Fresh", "Testdistrict");
    }

    @Test
    void shouldBeDueAtFourPmOnTheDayBeforeTheRun() {
        assertThat(job.triggerAt(RUN.runDate())).isEqualTo(Instant.parse("2026-09-30T10:30:00Z"));
    }

    @Test
    void shouldCloseTheOrdersAndConfirmTheFreshAmbientOnes() {
        Order freshAmbient = OrderFixtures.save(orders, OUTLET, "Fresh", TemperatureRequirement.AMBIENT,
                OrderStatus.PREPARED);
        Order tech = OrderFixtures.save(orders, OUTLET, "Tech", TemperatureRequirement.AMBIENT, OrderStatus.PREPARED);

        JobOutcome outcome = job.run(RUN);

        assertThat(outcome.isSkipped()).isFalse();
        assertThat(closeOrders.status(RUN.runDate(), RUN.depot()).closed()).isTrue();
        assertThat(statusOf(freshAmbient)).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(statusOf(tech)).isEqualTo(OrderStatus.PREPARED);
    }

    @Test
    void shouldCountAnAlreadyClosedRunAsSuccess() {
        closeOrders.close(LocalDate.parse("2026-10-01"), RUN.depot());
        Instant closedAt = closeOrders.status(RUN.runDate(), RUN.depot()).closedAt().toInstant();

        JobOutcome outcome = job.run(RUN);

        assertThat(outcome.isSkipped()).isTrue();
        assertThat(closeOrders.status(RUN.runDate(), RUN.depot()).closedAt().toInstant()).isEqualTo(closedAt);
    }

    private OrderStatus statusOf(Order order) {
        return orders.findById(order.getId()).orElseThrow().getStatus();
    }
}
