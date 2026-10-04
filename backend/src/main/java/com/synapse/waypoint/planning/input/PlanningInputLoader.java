package com.synapse.waypoint.planning.input;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.OrderFilters;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.reference.dto.OutletDto;
import com.synapse.waypoint.core.reference.dto.VehicleDto;
import com.synapse.waypoint.core.reference.service.ReferenceService;
import com.synapse.waypoint.planning.engine.input.InvalidPlanningInputException;
import com.synapse.waypoint.planning.engine.input.OrderInput;
import com.synapse.waypoint.planning.engine.input.OutletInput;
import com.synapse.waypoint.planning.engine.input.PlanningInput;
import com.synapse.waypoint.planning.engine.input.ServiceKey;
import com.synapse.waypoint.planning.engine.input.TravelInput;
import com.synapse.waypoint.planning.engine.input.VehicleInput;

/**
 * Builds the engine's input for one run date and depot. Read-only: only CONFIRMED orders are planned,
 * and everything comes through {@link OrderService} and {@link ReferenceService}.
 */
@Service
public class PlanningInputLoader {

    private final OrderService orderService;
    private final ReferenceService referenceService;

    public PlanningInputLoader(OrderService orderService, ReferenceService referenceService) {
        this.orderService = orderService;
        this.referenceService = referenceService;
    }

    @Transactional(readOnly = true)
    public PlanningInput load(LocalDate runDate, String depot) {
        List<OrderInput> orders = confirmedOrders(runDate, depot);
        Map<String, OutletInput> outlets = outletsOf(depot);
        List<VehicleInput> vehicles = availableVehicles(runDate, depot);
        Set<OutletInput> served = orders.stream()
                .map(order -> outletOf(order, outlets))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new PlanningInput(runDate, depot, orders, outlets, vehicles, travelFor(served, depot),
                serviceMinutesFor(orders, outlets));
    }

    private List<OrderInput> confirmedOrders(LocalDate runDate, String depot) {
        OrderFilters confirmedOnly = new OrderFilters(OrderStatus.CONFIRMED, null, null, null);
        return orderService.findByRun(runDate, depot, confirmedOnly).stream()
                .map(PlanningInputMapper::toOrderInput)
                .toList();
    }

    private Map<String, OutletInput> outletsOf(String depot) {
        return referenceService.outlets(depot).stream()
                .collect(Collectors.toMap(OutletDto::id, PlanningInputMapper::toOutletInput));
    }

    private List<VehicleInput> availableVehicles(LocalDate runDate, String depot) {
        int isoYear = runDate.get(IsoFields.WEEK_BASED_YEAR);
        int isoWeek = runDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return referenceService.availableVehicles(runDate, depot).stream()
                .map(vehicle -> withFuelUsed(vehicle, isoYear, isoWeek))
                .toList();
    }

    private VehicleInput withFuelUsed(VehicleDto vehicle, int isoYear, int isoWeek) {
        BigDecimal used = referenceService.fuelUsed(vehicle.id(), isoYear, isoWeek);
        return PlanningInputMapper.toVehicleInput(vehicle, used);
    }

    private Map<String, TravelInput> travelFor(Set<OutletInput> servedOutlets, String depot) {
        Map<String, TravelInput> travel = new HashMap<>();
        for (OutletInput outlet : servedOutlets) {
            travel.computeIfAbsent(outlet.district(), district ->
                    PlanningInputMapper.toTravelInput(referenceService.travel(district, depot)));
        }
        return travel;
    }

    private Map<ServiceKey, Integer> serviceMinutesFor(List<OrderInput> orders, Map<String, OutletInput> outlets) {
        Map<ServiceKey, Integer> minutes = new HashMap<>();
        for (OrderInput order : orders) {
            ServiceKey key = new ServiceKey(order.brand(), outletOf(order, outlets).dockType());
            minutes.computeIfAbsent(key, k ->
                    referenceService.serviceMinutes(k.brand().label(), k.dockType().value()));
        }
        return minutes;
    }

    private static OutletInput outletOf(OrderInput order, Map<String, OutletInput> outlets) {
        OutletInput outlet = outlets.get(order.outletId());
        if (outlet == null) {
            throw new InvalidPlanningInputException("Order " + order.ref() + " is for outlet " + order.outletId()
                    + ", which does not belong to the planned depot.");
        }
        return outlet;
    }
}
