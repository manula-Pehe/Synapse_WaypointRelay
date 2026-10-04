package com.synapse.waypoint.planning.engine.input;

import java.time.LocalTime;

import com.synapse.waypoint.planning.domain.DockType;
import com.synapse.waypoint.planning.domain.ParkingConstraint;

/** An outlet as the planner needs it; mall outlets use their mall window instead of the normal one. */
public record OutletInput(String id, String depot, String district, DockType dockType,
        ParkingConstraint parkingConstraint, LocalTime windowOpen, LocalTime windowClose, LocalTime mallWindowOpen,
        LocalTime mallWindowClose) {

    public DeliveryWindow deliveryWindow() {
        if (mallWindowOpen != null && mallWindowClose != null) {
            return new DeliveryWindow(mallWindowOpen, mallWindowClose);
        }
        return new DeliveryWindow(windowOpen, windowClose);
    }

    public boolean isVanOnly() {
        return parkingConstraint == ParkingConstraint.VAN_ONLY;
    }
}
