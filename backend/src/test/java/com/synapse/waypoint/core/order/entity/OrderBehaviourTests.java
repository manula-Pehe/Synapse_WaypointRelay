package com.synapse.waypoint.core.order.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.core.order.exception.InvalidStatusException;

class OrderBehaviourTests {

    private static final Instant T0 = Instant.parse("2026-09-30T08:30:00Z");
    private static final Instant T1 = T0.plusSeconds(60);

    @Test
    void shouldChangeStatusAndTimestampWhenTheTransitionIsAllowed() {
        Order order = orderWith(OrderStatus.CONFIRMED, 10);

        order.changeStatus(OrderStatus.PLANNED, T1);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLANNED);
        assertThat(order.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void shouldRefuseAForbiddenTransitionAndLeaveTheOrderUntouched() {
        Order order = orderWith(OrderStatus.PREPARED, 10);

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.LOADED, T1))
                .isInstanceOfSatisfying(InvalidStatusException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.INVALID_STATUS);
                    assertThat(e.details()).containsEntry("from", "PREPARED").containsEntry("to", "LOADED");
                });
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARED);
        assertThat(order.getUpdatedAt()).isEqualTo(T0);
    }

    @Test
    void shouldRecordWhoConfirmedAndWhen() {
        Order order = orderWith(OrderStatus.PREPARED, 10);

        order.confirm("usr-1", T1);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getConfirmedBy()).isEqualTo("usr-1");
        assertThat(order.getConfirmedAt()).isEqualTo(T1);
    }

    @Test
    void shouldScaleWeightAndVolumeWhenUnitsChange() {
        Order order = orderWith(OrderStatus.PREPARED, 10);

        order.editUnits(5, T1);

        assertThat(order.getUnits()).isEqualTo(5);
        assertThat(order.getWeightKg()).isEqualByComparingTo("50.00");
        assertThat(order.getVolumeM3()).isEqualByComparingTo("0.500");
    }

    @Test
    void shouldAllowEditingUnitsOnceConfirmed() {
        Order order = orderWith(OrderStatus.CONFIRMED, 10);

        order.editUnits(5, T1);
        assertThat(order.getUnits()).isEqualTo(5);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void shouldKeepTheSameOrderWhenMovedToANewDate() {
        Order order = orderWith(OrderStatus.CONFIRMED, 10);

        order.moveTo(LocalDate.parse("2026-10-02"), T1);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.MOVED);
        assertThat(order.getRunDate()).isEqualTo(LocalDate.parse("2026-10-02"));
    }

    private static Order orderWith(OrderStatus status, int units) {
        return Order.create(new NewOrder("o-1", "T-1", "OUT991", "Fresh", TemperatureRequirement.AMBIENT, units,
                new BigDecimal("100.00"), new BigDecimal("1.000"), LocalDate.parse("2026-10-01"), status,
                OrderSource.STORE, false, 1, false, null, true), T0);
    }
}
