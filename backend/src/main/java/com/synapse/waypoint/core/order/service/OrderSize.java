package com.synapse.waypoint.core.order.service;

import java.math.BigDecimal;

/** Estimated weight and volume of an order. */
record OrderSize(BigDecimal weightKg, BigDecimal volumeM3) {
}
