package com.synapse.waypoint.planning.choice;

import java.util.Map;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.core.order.dto.OrderDto;

/** Units a store sends with REDUCE or SPLIT: at least one, and fewer than the order has. */
final class ChoiceUnits {

    private ChoiceUnits() {
    }

    static void requireSmallerThanOrdered(OrderDto order, Integer units) {
        if (units == null || units < 1 || units >= order.units()) {
            throw new DomainException(ErrorCode.VALIDATION,
                    "Send a quantity of at least 1 and fewer than the " + order.units() + " ordered.",
                    Map.of("units", "must be between 1 and " + (order.units() - 1)));
        }
    }
}
