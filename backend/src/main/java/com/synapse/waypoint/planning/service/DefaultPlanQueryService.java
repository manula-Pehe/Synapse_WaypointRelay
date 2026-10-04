package com.synapse.waypoint.planning.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.planning.domain.PlanStatus;
import com.synapse.waypoint.planning.dto.DeferralDto;
import com.synapse.waypoint.planning.dto.PlanDto;
import com.synapse.waypoint.planning.dto.PlanTripDto;
import com.synapse.waypoint.planning.dto.StopPlacementDto;
import com.synapse.waypoint.planning.entity.Trip;
import com.synapse.waypoint.planning.repository.DeferralRepository;
import com.synapse.waypoint.planning.repository.PlanRepository;
import com.synapse.waypoint.planning.repository.StopRepository;
import com.synapse.waypoint.planning.repository.TripRepository;

@Service
@Transactional(readOnly = true)
class DefaultPlanQueryService implements PlanQueryService {

    private final PlanRepository plans;
    private final TripRepository trips;
    private final StopRepository stops;
    private final DeferralRepository deferrals;
    private final PlanViewLoader views;

    DefaultPlanQueryService(PlanRepository plans, TripRepository trips, StopRepository stops,
            DeferralRepository deferrals, PlanViewLoader views) {
        this.plans = plans;
        this.trips = trips;
        this.stops = stops;
        this.deferrals = deferrals;
        this.views = views;
    }

    @Override
    public List<PlanTripDto> tripsForVehicle(LocalDate runDate, String vehicleId) {
        return views.trips(trips.findPublishedByRunDateAndVehicle(runDate, vehicleId));
    }

    @Override
    public List<PlanTripDto> tripsForDepot(LocalDate runDate, String depot) {
        return plans.findFirstByRunDateAndDepotIgnoreCaseAndStatus(runDate, depot, PlanStatus.PUBLISHED)
                .map(plan -> views.trips(trips.findByPlanIdOrderByVehicleIdAscTripNoAsc(plan.getId())))
                .orElse(List.of());
    }

    @Override
    public Optional<StopPlacementDto> stopForOrder(String orderId) {
        List<Trip> publishedTrips = trips.findPublishedByOrder(orderId);
        return stops.findByOrderId(orderId).stream()
                .flatMap(stop -> publishedTrips.stream()
                        .filter(trip -> trip.getId().equals(stop.getTripId()))
                        .map(trip -> views.placement(trip, stop)))
                .findFirst();
    }

    @Override
    public Optional<DeferralDto> deferralForOrder(String orderId) {
        return deferrals.findPublishedByOrder(orderId).stream().findFirst()
                .map(deferral -> views.toDtos(List.of(deferral)).get(0));
    }

    @Override
    public List<DeferralDto> deferralsForOutlet(LocalDate runDate, String outletId) {
        return views.toDtos(deferrals.findPublishedByRunDate(runDate)).stream()
                .filter(deferral -> deferral.outletId().equals(outletId))
                .toList();
    }

    @Override
    public Optional<PlanDto> publishedPlan(LocalDate runDate, String depot) {
        return plans.findFirstByRunDateAndDepotIgnoreCaseAndStatus(runDate, depot, PlanStatus.PUBLISHED)
                .map(views::plan);
    }
}
