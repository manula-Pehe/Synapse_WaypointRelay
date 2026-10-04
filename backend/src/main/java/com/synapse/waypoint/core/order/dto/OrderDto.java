package com.synapse.waypoint.core.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.synapse.waypoint.core.order.entity.OrderSource;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;

/** The order object of docs/api.md §4; times are Sri Lanka time in whole seconds. */
public record OrderDto(
        String id,
        String ref,
        String outletId,
        String outletName,
        String brand,
        TemperatureRequirement temp,
        int units,
        BigDecimal weightKg,
        BigDecimal volumeM3,
        LocalDate runDate,
        OrderStatus status,
        OrderSource source,
        boolean autoConfirm,
        int daysSinceLastServed,
        boolean deferredYesterday,
        String parentOrderId,
        boolean storeChecked,
        OffsetDateTime confirmedAt,
        OffsetDateTime updatedAt) {
}
