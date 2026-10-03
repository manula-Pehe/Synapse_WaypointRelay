package com.synapse.waypoint.core.order.dto;

import java.time.OffsetDateTime;

/** {@code GET /api/orders/close-status}; {@code closedBy} is null when a timed job closed the orders. */
public record CloseStatusDto(boolean closed, OffsetDateTime closedAt, String closedBy, OffsetDateTime cutOffAt) {
}
