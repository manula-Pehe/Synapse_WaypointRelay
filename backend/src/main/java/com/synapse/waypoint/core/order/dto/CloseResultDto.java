package com.synapse.waypoint.core.order.dto;

import java.time.OffsetDateTime;

/**
 * What closing orders did, for one run and depot: orders already confirmed, orders confirmed
 * automatically by the cut-off, and orders still unconfirmed (left out of the run).
 */
public record CloseResultDto(OffsetDateTime closedAt, int confirmed, int autoConfirmed, int notConfirmed) {
}
