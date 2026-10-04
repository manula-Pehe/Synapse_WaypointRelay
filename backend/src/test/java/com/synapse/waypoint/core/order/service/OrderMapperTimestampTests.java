package com.synapse.waypoint.core.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.time.ApiTimestamp;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.OrderEventDto;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.support.OrderFixtures;

/** Order and history times are shown like GET /api/settings: Sri Lanka offset, whole seconds. */
@SpringBootTest
@Transactional
class OrderMapperTimestampTests {

    private static final String OUTLET = "OUT991";
    private static final ZoneOffset SRI_LANKA = ZoneOffset.ofHoursMinutes(5, 30);

    @Autowired OrderService service;
    @Autowired OrderRepository orders;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void createOutlet() {
        OrderFixtures.insertOutlet(jdbc, OUTLET, "Fresh", "Testdistrict");
    }

    @Test
    void shouldShowOrderTimesInSriLankaTimeInWholeSeconds() {
        Order order = OrderFixtures.save(orders, OUTLET, OrderStatus.PREPARED);

        OrderDto confirmed = service.confirm(order.getId());

        assertThat(confirmed.updatedAt().getOffset()).isEqualTo(SRI_LANKA);
        assertThat(confirmed.updatedAt().getNano()).isZero();
        assertThat(confirmed.confirmedAt().getOffset()).isEqualTo(SRI_LANKA);
        assertThat(confirmed.confirmedAt().getNano()).isZero();
    }

    @Test
    void shouldShowHistoryTimesInSriLankaTimeInWholeSeconds() {
        Order order = OrderFixtures.save(orders, OUTLET, OrderStatus.PREPARED);
        service.confirm(order.getId());

        OrderEventDto event = service.history(order.getId()).get(0);

        assertThat(event.at().getOffset()).isEqualTo(SRI_LANKA);
        assertThat(event.at().getNano()).isZero();
    }

    @Test
    void shouldKeepAnUnsetTimeEmpty() {
        Order order = OrderFixtures.save(orders, OUTLET, OrderStatus.PREPARED);

        assertThat(service.get(order.getId()).confirmedAt()).isNull();
        assertThat(ApiTimestamp.of(null)).isNull();
    }

    @Test
    void shouldTruncateWithoutRoundingAndKeepTheSameInstant() {
        Instant instant = Instant.parse("2026-09-30T10:29:59.987654Z");

        OffsetDateTime shown = ApiTimestamp.of(instant);

        assertThat(shown).isEqualTo(OffsetDateTime.parse("2026-09-30T15:59:59+05:30"));
    }
}
