package com.synapse.waypoint.planning.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.reference.dto.VehicleDto;
import com.synapse.waypoint.core.reference.service.ReferenceService;
import com.synapse.waypoint.planning.dto.DeferralDto;
import com.synapse.waypoint.planning.dto.PlanDto;
import com.synapse.waypoint.planning.dto.PlanTripDto;
import com.synapse.waypoint.planning.dto.StopPlacementDto;
import com.synapse.waypoint.planning.domain.StoreChoice;
import com.synapse.waypoint.planning.entity.Deferral;
import com.synapse.waypoint.planning.entity.DeferralChoice;
import com.synapse.waypoint.planning.entity.Plan;
import com.synapse.waypoint.planning.entity.Stop;
import com.synapse.waypoint.planning.entity.Trip;
import com.synapse.waypoint.planning.repository.DeferralChoiceRepository;
import com.synapse.waypoint.planning.repository.DeferralRepository;
import com.synapse.waypoint.planning.repository.StopRepository;
import com.synapse.waypoint.planning.repository.TripRepository;

/**
 * Builds the DTOs of a stored plan. Orders and vehicles are looked up once per call and are not
 * limited to the signed-in user, because read callers include drivers, loaders and store managers.
 */
@Component
class PlanViewLoader {

    private final TripRepository trips;
    private final StopRepository stops;
    private final DeferralRepository deferrals;
    private final DeferralChoiceRepository choices;
    private final OrderService orders;
    private final ReferenceService reference;
    private final PlanMapper mapper;

    PlanViewLoader(TripRepository trips, StopRepository stops, DeferralRepository deferrals,
            DeferralChoiceRepository choices, OrderService orders, ReferenceService reference, PlanMapper mapper) {
        this.trips = trips;
        this.stops = stops;
        this.deferrals = deferrals;
        this.choices = choices;
        this.orders = orders;
        this.reference = reference;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PlanDto plan(Plan plan) {
        List<Trip> planTrips = trips.findByPlanIdOrderByVehicleIdAscTripNoAsc(plan.getId());
        Map<String, List<Stop>> stopsByTrip = stopsByTrip(planTrips);
        return mapper.toDto(plan, planTrips, stopsByTrip, ordersOf(stopsByTrip), vehiclesOf(plan));
    }

    @Transactional(readOnly = true)
    public List<PlanTripDto> trips(List<Trip> selected) {
        Map<String, List<Stop>> stopsByTrip = stopsByTrip(selected);
        Map<String, OrderDto> ordersById = ordersOf(stopsByTrip);
        return selected.stream()
                .map(trip -> mapper.toTrip(trip, stopsByTrip.getOrDefault(trip.getId(), List.of()), ordersById))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeferralDto> deferrals(String planId) {
        return toDtos(deferrals.findByPlanId(planId));
    }

    @Transactional(readOnly = true)
    public List<DeferralDto> toDtos(List<Deferral> found) {
        Map<String, OrderDto> ordersById = ordersById(found.stream().map(Deferral::getOrderId).toList());
        Map<String, StoreChoice> choices = choicesOf(found);
        return found.stream()
                .map(deferral -> mapper.toDeferral(deferral, ordersById.get(deferral.getOrderId()),
                        choices.get(deferral.getId())))
                .sorted(Comparator.comparing(DeferralDto::orderRef))
                .toList();
    }

    @Transactional(readOnly = true)
    public StopPlacementDto placement(Trip trip, Stop stop) {
        return mapper.toPlacement(trip, stop, ordersById(List.of(stop.getOrderId())).get(stop.getOrderId()));
    }

    private Map<String, List<Stop>> stopsByTrip(List<Trip> planTrips) {
        if (planTrips.isEmpty()) {
            return Map.of();
        }
        return stops.findByTripIdInOrderByTripIdAscSeqAsc(planTrips.stream().map(Trip::getId).toList()).stream()
                .collect(Collectors.groupingBy(Stop::getTripId));
    }

    private Map<String, OrderDto> ordersOf(Map<String, List<Stop>> stopsByTrip) {
        return ordersById(stopsByTrip.values().stream().flatMap(List::stream).map(Stop::getOrderId).toList());
    }

    private Map<String, StoreChoice> choicesOf(List<Deferral> found) {
        if (found.isEmpty()) {
            return Map.of();
        }
        return choices.findByDeferralIdIn(found.stream().map(Deferral::getId).toList()).stream()
                .collect(Collectors.toMap(DeferralChoice::getDeferralId, DeferralChoice::getChoice,
                        (first, second) -> first));
    }

    private Map<String, OrderDto> ordersById(Collection<String> orderIds) {
        return orders.findByIds(orderIds).stream().collect(Collectors.toMap(OrderDto::id, Function.identity()));
    }

    private Map<String, VehicleDto> vehiclesOf(Plan plan) {
        return reference.vehicles(plan.getRunDate(), plan.getDepot()).stream()
                .collect(Collectors.toMap(VehicleDto::id, Function.identity()));
    }
}
