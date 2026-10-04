package com.synapse.waypoint.core.order.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code POST /api/dispatch/orders/close} - the D1 button. */
public record CloseOrdersRequest(@NotNull LocalDate runDate, @NotBlank @Size(max = 20) String depot) {
}
