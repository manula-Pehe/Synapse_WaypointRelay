package com.synapse.waypoint.planning.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.time.ApiTimestamp;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.reference.dto.VehicleDto;
import com.synapse.waypoint.planning.domain.StoreChoice;
import com.synapse.waypoint.planning.domain.TripWindowType;
import com.synapse.waypoint.planning.dto.DeferralDto;
import com.synapse.waypoint.planning.dto.PlanDto;
import com.synapse.waypoint.planning.dto.PlanStopDto;
import com.synapse.waypoint.planning.dto.PlanSummaryDto;
import com.synapse.waypoint.planning.dto.PlanTripDto;
import com.synapse.waypoint.planning.dto.PlanVehicleDto;
import com.synapse.waypoint.planning.dto.StopPlacementDto;
import com.synapse.waypoint.planning.entity.Deferral;
import com.synapse.waypoint.planning.entity.Plan;
import com.synapse.waypoint.planning.entity.Stop;
import com.synapse.waypoint.planning.entity.Trip;

/**
 * Entity to DTO conversion. Orders and vehicles arrive as maps loaded once by the caller, so
 * mapping never queries per stop or per vehicle.
 */
@Component
class PlanMapper {

    PlanDto toDto(Plan plan, List<Trip> trips, Map<String, List<Stop>> stopsByTrip,
            Map<String, OrderDto> ordersById, Map<String, VehicleDto> vehiclesById) {
        List<PlanVehicleDto> vehicles = trips.stream()
                .collect(Collectors.groupingBy(Trip::getVehicleId))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toVehicle(vehicle(vehiclesById, entry.getKey()), entry.getValue(), stopsByTrip,
                        ordersById))
                .toList();
        return new PlanDto(plan.getId(), plan.getRunDate(), plan.getDepot(), plan.getVersion(), plan.getStatus(),
                PlanSummaryDto.fromStored(plan.getSummary()), vehicles);
    }

    PlanTripDto toTrip(Trip trip, List<Stop> stops, Map<String, OrderDto> ordersById) {
        List<PlanStopDto> stopDtos = stops.stream()
                .sorted(Comparator.comparingInt(Stop::getSeq))
                .map(stop -> toStop(stop, order(ordersById, stop.getOrderId())))
                .toList();
        return new PlanTripDto(trip.getId(), trip.getTripNo(), trip.getBrand(), trip.getDistrict(),
                trip.getWindowType(), ApiTimestamp.of(trip.getDepartAt()), trip.getMinutes(), trip.getWeightKg(),
                trip.getVolumeM3(), stopDtos);
    }

    StopPlacementDto toPlacement(Trip trip, Stop stop, OrderDto order) {
        return new StopPlacementDto(trip.getId(), trip.getVehicleId(), trip.getTripNo(),
                ApiTimestamp.of(trip.getDepartAt()), toStop(stop, order));
    }

    DeferralDto toDeferral(Deferral deferral, OrderDto order, StoreChoice storeChoice) {
        return new DeferralDto(deferral.getId(), deferral.getOrderId(), order.ref(), order.outletId(),
                deferral.getKind(), deferral.getRule(), deferral.getReason(), deferral.getPriorityScore(),
                deferral.getDaysWaited(), deferral.getNewDate(), deferral.isNeedsDecision(), storeChoice);
    }

    private PlanVehicleDto toVehicle(VehicleDto vehicle, List<Trip> trips, Map<String, List<Stop>> stopsByTrip,
            Map<String, OrderDto> ordersById) {
        List<PlanTripDto> tripDtos = trips.stream()
                .sorted(Comparator.comparingInt(Trip::getTripNo))
                .map(trip -> toTrip(trip, stopsByTrip.getOrDefault(trip.getId(), List.of()), ordersById))
                .toList();
        return new PlanVehicleDto(vehicle.id(), vehicle.type(), vehicle.temp(), vehicle.weightCapKg(),
                vehicle.volumeCapM3(), minutesUsed(trips, TripWindowType.FRESH), TripWindowType.FRESH.budgetMinutes(),
                minutesUsed(trips, TripWindowType.DAYTIME), TripWindowType.DAYTIME.budgetMinutes(), tripDtos);
    }

    private static int minutesUsed(List<Trip> trips, TripWindowType window) {
        return trips.stream().filter(trip -> trip.getWindowType() == window).mapToInt(Trip::getMinutes).sum();
    }

    private static PlanStopDto toStop(Stop stop, OrderDto order) {
        return new PlanStopDto(stop.getId(), stop.getOrderId(), order.ref(), order.outletId(), stop.getSeq(),
                stop.getLoadSeq(), order.units(), order.temp().name(), ApiTimestamp.of(stop.getArriveFrom()),
                ApiTimestamp.of(stop.getArriveTo()), stop.getLateRisk());
    }

    private static OrderDto order(Map<String, OrderDto> ordersById, String orderId) {
        OrderDto order = ordersById.get(orderId);
        if (order == null) {
            throw new NotFoundException("Order", orderId);
        }
        return order;
    }

    private static VehicleDto vehicle(Map<String, VehicleDto> vehiclesById, String vehicleId) {
        VehicleDto vehicle = vehiclesById.get(vehicleId);
        if (vehicle == null) {
            throw new NotFoundException("Vehicle", vehicleId);
        }
        return vehicle;
    }
}
