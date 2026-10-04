package com.synapse.waypoint.core.reference.dto;

import java.math.BigDecimal;

/** Distances and free-flow times from a depot to a district, as the trip-time formula needs them. */
public record TravelDto(String district, String depot, String roadClass, BigDecimal freeFlowKmh,
        BigDecimal depotToDistrictKm, int outboundMinutes, BigDecimal interStopKm, int interStopMinutes) {
}
