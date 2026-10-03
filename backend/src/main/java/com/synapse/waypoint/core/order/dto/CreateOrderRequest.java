package com.synapse.waypoint.core.order.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.synapse.waypoint.core.order.entity.TemperatureRequirement;

/**
 * A new order stating only its quantity: a store's extra order (S2n) or one the dispatcher took
 * by phone (D1b). Weight and volume are estimated; {@code note} is kept in the order's history.
 */
public record CreateOrderRequest(
        @NotBlank @Size(max = 10) String outletId,
        @NotNull LocalDate runDate,
        @NotNull TemperatureRequirement temp,
        @Positive int units,
        @Size(max = 500) String note) {
}
