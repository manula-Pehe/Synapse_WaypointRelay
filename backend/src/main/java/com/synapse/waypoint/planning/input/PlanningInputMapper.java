package com.synapse.waypoint.planning.input;

import java.math.BigDecimal;

import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.reference.dto.OutletDto;
import com.synapse.waypoint.core.reference.dto.TravelDto;
import com.synapse.waypoint.core.reference.dto.VehicleDto;
import com.synapse.waypoint.planning.domain.Brand;
import com.synapse.waypoint.planning.domain.DockType;
import com.synapse.waypoint.planning.domain.ParkingConstraint;
import com.synapse.waypoint.planning.domain.Temperature;
import com.synapse.waypoint.planning.domain.VehicleTemperature;
import com.synapse.waypoint.planning.domain.VehicleType;
import com.synapse.waypoint.planning.engine.input.OrderInput;
import com.synapse.waypoint.planning.engine.input.OutletInput;
import com.synapse.waypoint.planning.engine.input.TravelInput;
import com.synapse.waypoint.planning.engine.input.VehicleInput;


/** Converts the DTOs of the order and reference services into the engine's input records. */
final class PlanningInputMapper {

    private PlanningInputMapper() {
    }

    static OrderInput toOrderInput(OrderDto order) {
        return new OrderInput(order.id(), order.ref(), order.outletId(), Brand.fromLabel(order.brand()),
                Temperature.valueOf(order.temp().name()), order.weightKg(), order.volumeM3(),
                order.daysSinceLastServed(), order.deferredYesterday());
    }

    static OutletInput toOutletInput(OutletDto outlet) {
        return new OutletInput(outlet.id(), outlet.depot(), outlet.district(), DockType.fromValue(outlet.dockType()),
                ParkingConstraint.fromValue(outlet.parkingConstraint()), outlet.windowOpen(), outlet.windowClose(),
                outlet.mallWindowOpen(), outlet.mallWindowClose());
    }

    static VehicleInput toVehicleInput(VehicleDto vehicle, BigDecimal fuelUsedThisWeekLitres) {
        return new VehicleInput(vehicle.id(), vehicle.depot(), VehicleType.fromValue(vehicle.type()),
                VehicleTemperature.fromValue(vehicle.temp()), vehicle.weightCapKg(), vehicle.volumeCapM3(),
                vehicle.kmPerL(), vehicle.weeklyFuelQuotaL(), fuelUsedThisWeekLitres);
    }

    static TravelInput toTravelInput(TravelDto travel) {
        return new TravelInput(travel.district(), travel.depotToDistrictKm(), travel.outboundMinutes(),
                travel.interStopKm(), travel.interStopMinutes());
    }
}
