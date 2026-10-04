package com.synapse.waypoint.planning.engine.input;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.synapse.waypoint.planning.domain.Brand;
import com.synapse.waypoint.planning.domain.DockType;

/**
 * Everything the engine needs for one run date and depot. The run date is the only source of
 * "today": the engine never reads the system clock.
 */
public record PlanningInput(LocalDate runDate, String depot, List<OrderInput> orders,
        Map<String, OutletInput> outletsById, List<VehicleInput> vehicles, Map<String, TravelInput> travelByDistrict,
        Map<ServiceKey, Integer> serviceMinutes) {

    public PlanningInput {
        orders = List.copyOf(orders);
        outletsById = Map.copyOf(outletsById);
        vehicles = List.copyOf(vehicles);
        travelByDistrict = Map.copyOf(travelByDistrict);
        serviceMinutes = Map.copyOf(serviceMinutes);
    }

    public OutletInput outletOf(OrderInput order) {
        OutletInput outlet = outletsById.get(order.outletId());
        if (outlet == null) {
            throw new InvalidPlanningInputException("No outlet " + order.outletId() + " for order " + order.ref());
        }
        return outlet;
    }

    public TravelInput travel(String district) {
        TravelInput travel = travelByDistrict.get(district);
        if (travel == null) {
            throw new InvalidPlanningInputException("No travel times for district " + district);
        }
        return travel;
    }

    public int serviceMinutes(Brand brand, DockType dockType) {
        Integer minutes = serviceMinutes.get(new ServiceKey(brand, dockType));
        if (minutes == null) {
            throw new InvalidPlanningInputException("No service time for " + brand + " at " + dockType);
        }
        return minutes;
    }
}
