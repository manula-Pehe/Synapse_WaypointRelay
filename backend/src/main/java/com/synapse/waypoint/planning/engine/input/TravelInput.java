package com.synapse.waypoint.planning.engine.input;

import java.math.BigDecimal;

/** Distances and free-flow minutes from the depot to one district. */
public record TravelInput(String district, BigDecimal depotToDistrictKm, int outboundMinutes,
        BigDecimal interStopKm, int interStopMinutes) {
}
