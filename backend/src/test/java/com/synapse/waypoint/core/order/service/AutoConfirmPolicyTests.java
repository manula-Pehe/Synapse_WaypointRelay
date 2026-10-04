package com.synapse.waypoint.core.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.synapse.waypoint.core.order.entity.NewOrder;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderSource;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.support.OrderFixtures;

class AutoConfirmPolicyTests {

    private final AutoConfirmPolicy policy = new AutoConfirmPolicy();

    static Stream<Arguments> brandsAndTemperatures() {
        return Stream.of(
                Arguments.of("Fresh", TemperatureRequirement.AMBIENT, true),
                Arguments.of("fresh", TemperatureRequirement.AMBIENT, true),
                Arguments.of("Fresh", TemperatureRequirement.CHILLED, false),
                Arguments.of("Style", TemperatureRequirement.AMBIENT, false),
                Arguments.of("Style", TemperatureRequirement.CHILLED, false),
                Arguments.of("Tech", TemperatureRequirement.AMBIENT, false),
                Arguments.of("Tech", TemperatureRequirement.CHILLED, false));
    }

    @ParameterizedTest
    @MethodSource("brandsAndTemperatures")
    void shouldAutoConfirmOnlyFreshAmbientOrders(String brand, TemperatureRequirement temp, boolean expected) {
        assertThat(policy.appliesTo(order(brand, temp))).isEqualTo(expected);
    }

    private static Order order(String brand, TemperatureRequirement temp) {
        NewOrder base = OrderFixtures.newOrder("OUT991", OrderStatus.PREPARED, temp, 10, OrderSource.SEED,
                OrderFixtures.RUN_DATE);
        return Order.create(new NewOrder(base.id(), base.ref(), base.outletId(), brand, temp, base.units(),
                new BigDecimal("100.00"), new BigDecimal("1.000"), base.runDate(), base.status(), base.source(),
                false, 1, false, null, true), OrderFixtures.CREATED_AT);
    }
}
