package com.synapse.waypoint.core.order.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.synapse.waypoint.core.order.entity.OrderStatus.*;

/** The lifecycle table in docs/api.md, checked over every pair of statuses. */
class OrderStatusTests {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            PREPARED, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(PLANNED, MOVED, CANCELLED),
            PLANNED, Set.of(LOADED, MOVED),
            MOVED, Set.of(CONFIRMED, CANCELLED),
            LOADED, Set.of(ON_THE_WAY),
            ON_THE_WAY, Set.of(DELIVERED, PARTIAL, FAILED),
            FAILED, Set.of(MOVED, CANCELLED));

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @MethodSource("allowedPairs")
    void shouldAllowEveryTransitionInTheLifecycle(OrderStatus from, OrderStatus to) {
        assertThat(from.canMoveTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} is refused")
    @MethodSource("forbiddenPairs")
    void shouldRefuseEveryOtherTransition(OrderStatus from, OrderStatus to) {
        assertThat(from.canMoveTo(to)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("endStatuses")
    void shouldHaveNoNextStatusAfterAnEndState(OrderStatus end) {
        assertThat(end.allowedNext()).isEmpty();
    }

    static Stream<Arguments> allowedPairs() {
        return pairs(true);
    }

    static Stream<Arguments> forbiddenPairs() {
        return pairs(false);
    }

    static List<OrderStatus> endStatuses() {
        return List.of(DELIVERED, PARTIAL, CANCELLED);
    }

    private static Stream<Arguments> pairs(boolean allowed) {
        return EnumSet.allOf(OrderStatus.class).stream()
                .flatMap(from -> EnumSet.allOf(OrderStatus.class).stream()
                        .filter(to -> ALLOWED.getOrDefault(from, Set.of()).contains(to) == allowed)
                        .map(to -> Arguments.of(from, to)));
    }
}
