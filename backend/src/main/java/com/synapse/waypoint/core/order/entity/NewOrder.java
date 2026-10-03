package com.synapse.waypoint.core.order.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Everything needed to create an {@link Order}; the entity assigns its own timestamps and version. */
public record NewOrder(
        String id,
        String ref,
        String outletId,
        String brand,
        TemperatureRequirement temperatureRequirement,
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
        boolean storeChecked) {
}
