package com.synapse.waypoint.planning.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** One outlet delivery inside a trip (docs/api.md §6); {@code loadSeq} is the loading order. */
public record PlanStopDto(String id, String orderId, String orderRef, String outletId, int seq, int loadSeq,
        int units, String temp, OffsetDateTime arriveFrom, OffsetDateTime arriveTo, BigDecimal lateRisk) {
}
