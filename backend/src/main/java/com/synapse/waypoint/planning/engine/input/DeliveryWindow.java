package com.synapse.waypoint.planning.engine.input;

import java.time.LocalTime;

/** The wall-clock span in which an outlet accepts deliveries. */
public record DeliveryWindow(LocalTime open, LocalTime close) {

    public boolean contains(LocalTime time) {
        return !time.isBefore(open) && !time.isAfter(close);
    }
}
