package com.synapse.waypoint.planning.engine;

import com.synapse.waypoint.planning.engine.input.DeliveryWindow;
import com.synapse.waypoint.planning.engine.input.OrderInput;
import com.synapse.waypoint.planning.engine.input.OutletInput;

/** An order together with the outlet it goes to, as one stop of a trip. */
public record StopCandidate(OrderInput order, OutletInput outlet) {

    public DeliveryWindow window() {
        return outlet.deliveryWindow();
    }
}
