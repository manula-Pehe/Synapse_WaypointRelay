package com.synapse.waypoint.core.order.dto;

import java.time.Instant;
import java.util.Map;

import com.synapse.waypoint.core.order.entity.OrderStatus;

/** One history row; {@code actor} is the user id, or null when the system made the change. */
public record OrderEventDto(
        Instant at,
        String actor,
        String type,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        Map<String, Object> details) {
}
