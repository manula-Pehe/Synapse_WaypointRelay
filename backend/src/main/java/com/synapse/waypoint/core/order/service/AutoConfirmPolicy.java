package com.synapse.waypoint.core.order.service;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;

/**
 * The cut-off rule for orders nobody confirmed: Fresh ambient orders are confirmed for the store.
 * Chilled, Style and Tech orders never are — an unconfirmed one is left out of the run.
 */
@Component
class AutoConfirmPolicy {

    private static final String FRESH_BRAND = "Fresh";

    boolean appliesTo(Order order) {
        return FRESH_BRAND.equalsIgnoreCase(order.getBrand())
                && order.getTemperatureRequirement() == TemperatureRequirement.AMBIENT;
    }
}
