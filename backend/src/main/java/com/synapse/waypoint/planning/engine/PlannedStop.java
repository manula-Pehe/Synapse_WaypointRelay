package com.synapse.waypoint.planning.engine;

import java.math.BigDecimal;
import java.time.LocalTime;

import com.synapse.waypoint.planning.engine.input.OrderInput;

/** One delivery of a planned trip; {@code loadSeq} is the reverse of {@code seq} so the last stop loads first. */
public record PlannedStop(OrderInput order, int seq, int loadSeq, LocalTime arriveFrom, LocalTime arriveTo,
        BigDecimal lateRisk) {
}
