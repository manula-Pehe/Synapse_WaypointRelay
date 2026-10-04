package com.synapse.waypoint.planning.engine;

import java.math.BigDecimal;
import java.time.LocalTime;

/** The arrival window shown to a store and how likely the truck is to miss the outlet's window (0–1). */
public record ArrivalWindow(LocalTime from, LocalTime to, BigDecimal lateRisk) {
}
