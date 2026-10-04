package com.synapse.waypoint.planning.engine;

import com.synapse.waypoint.planning.domain.VehicleType;
import com.synapse.waypoint.planning.engine.input.OrderInput;
import com.synapse.waypoint.planning.engine.input.OutletInput;
import com.synapse.waypoint.planning.engine.input.PlanningInput;
import com.synapse.waypoint.planning.engine.input.VehicleInput;

/**
 * How many available vehicles could carry an order on their own, judged by the rules that depend only
 * on the order and the vehicle: depot, temperature, van-only access and capacity.
 */
class CarrierCounter {

    private final PlanningInput input;

    CarrierCounter(PlanningInput input) {
        this.input = input;
    }

    int countFor(OrderInput order) {
        OutletInput outlet = input.outletOf(order);
        return (int) input.vehicles().stream().filter(vehicle -> canCarryAlone(order, outlet, vehicle)).count();
    }

    private boolean canCarryAlone(OrderInput order, OutletInput outlet, VehicleInput vehicle) {
        return outlet.depot().equalsIgnoreCase(vehicle.depot())
                && (!order.isChilled() || vehicle.isReefer())
                && (!outlet.isVanOnly() || vehicle.type() == VehicleType.VAN)
                && order.weightKg().compareTo(vehicle.weightCapKg()) <= 0
                && order.volumeM3().compareTo(vehicle.volumeCapM3()) <= 0;
    }
}
