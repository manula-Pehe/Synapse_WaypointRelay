package com.synapse.waypoint.driver.service;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.reference.dto.OutletDto;
import com.synapse.waypoint.core.reference.dto.VehicleDto;
import com.synapse.waypoint.core.reference.service.ReferenceService;
import com.synapse.waypoint.driver.dto.DriverStopDto;
import com.synapse.waypoint.driver.dto.DriverTripDto;
import com.synapse.waypoint.driver.dto.TodayDto;
import com.synapse.waypoint.driver.entity.TripRun;
import com.synapse.waypoint.driver.repository.TripRunRepository;
import com.synapse.waypoint.planning.dto.PlanStopDto;
import com.synapse.waypoint.planning.dto.PlanTripDto;
import com.synapse.waypoint.planning.dto.StopPlacementDto;
import com.synapse.waypoint.planning.service.PlanQueryService;

/**
 * Builds the driver's day from the published plan (docs/api.md §7).
 *
 * Two rules run through the whole class. The plan is only ever read through the planning module's service, so a
 * driver follows the published plan and not a draft. And the vehicle always comes from the signed-in
 * user, never from a parameter, so one driver cannot ask for another's run by guessing a vehicle id.
 */
@Service
@Transactional
class DefaultTodayService implements TodayService {

    private static final DateTimeFormatter WALL_CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private final PlanQueryService plans;
    private final ReferenceService reference;
    private final OrderService orders;
    private final TripRunRepository tripRuns;
    private final CurrentUser currentUser;
    private final DemoClock clock;

    DefaultTodayService(PlanQueryService plans, ReferenceService reference, OrderService orders,
            TripRunRepository tripRuns, CurrentUser currentUser, DemoClock clock) {
        this.plans = plans;
        this.reference = reference;
        this.orders = orders;
        this.tripRuns = tripRuns;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public TodayDto today() {
        String vehicleId = ownVehicle();
        var runDate = clock.runDate();

        List<PlanTripDto> trips = plans.tripsForVehicle(runDate, vehicleId);
        List<DriverTripDto> shaped = trips.stream().map(this::shape).toList();

        VehicleDto vehicle = reference.vehicle(vehicleId);
        String firstTripId = trips.isEmpty() ? null : trips.getFirst().id();
        Boolean accepted = firstTripId == null
                ? null
                : tripRuns.findById(firstTripId).map(TripRun::getAcceptedAt).map(at -> Boolean.TRUE).orElse(null);

        return new TodayDto(
                runDate,
                vehicleId,
                vehicle == null ? null : vehicle.type(),
                null,
                accepted,
                shaped);
    }

    @Override
    public TodayDto acceptTrip(String tripId) {
        String vehicleId = ownVehicle();

        boolean mine = plans.tripsForVehicle(clock.runDate(), vehicleId).stream()
                .anyMatch(trip -> trip.id().equals(tripId));
        if (!mine) {
            // Checked before any write: a driver may only accept the load of their own vehicle.
            throw new DomainException(ErrorCode.VALIDATION, "that trip is not on this vehicle");
        }

        // Accepted is recorded once; accepting again is a no-op rather than a second timestamp.
        tripRuns.findById(tripId)
                .orElseGet(() -> tripRuns.save(TripRun.accepted(tripId, currentUser.id(), clock.now())));

        // The orders move through OrderService so the lifecycle and its history row stay in one place.
        plans.tripsForVehicle(clock.runDate(), vehicleId).stream()
                .filter(trip -> trip.id().equals(tripId))
                .flatMap(trip -> trip.stops().stream())
                .forEach(stop -> orders.markOnTheWay(stop.orderId()));

        return today();
    }

    /**
     * The vehicle the signed-in user drives.
     *
     * Every read is scoped to this, which is what stops one driver reading another's run.
     */
    private String ownVehicle() {
        return currentUser.vehicleId().orElseThrow(() -> new DomainException(ErrorCode.FORBIDDEN,
                "this account has no vehicle, so it has no run"));
    }

    private DriverTripDto shape(PlanTripDto trip) {
        return new DriverTripDto(
                trip.id(),
                trip.tripNo(),
                trip.brand(),
                trip.district(),
                trip.departAt(),
                trip.stops().stream().map(this::shape).toList());
    }

    private DriverStopDto shape(PlanStopDto stop) {
        OutletDto outlet = reference.outlet(stop.outletId());
        Optional<StopPlacementDto> placement = plans.stopForOrder(stop.orderId());
        return new DriverStopDto(
                stop.id(),
                stop.seq(),
                stop.orderId(),
                stop.orderRef(),
                stop.outletId(),
                outlet == null ? stop.outletId() : outlet.name(),
                outlet == null ? null : outlet.district(),
                outlet == null ? null : outlet.dockType(),
                wallClock(outlet, true),
                wallClock(outlet, false),
                stop.units(),
                stop.temp(),
                stop.arriveFrom(),
                stop.arriveTo(),
                minutesEarly(stop, outlet),
                // The plan moved this order to another vehicle, so this stop is stale (R2c).
                placement.isPresent() && !stop.id().equals(placement.get().stop().id()));
    }

    /** R3a: how long the driver has to wait, so nobody unloads into a shut shop. */
    private int minutesEarly(PlanStopDto stop, OutletDto outlet) {
        if (outlet == null || outlet.windowOpen() == null || stop.arriveFrom() == null) {
            return 0;
        }
        LocalTime opening = outlet.windowOpen();
        LocalTime arrival = stop.arriveFrom().toLocalTime();
        return Math.toIntExact(Duration.between(arrival, opening).toMinutes());
    }


    private String wallClock(OutletDto outlet, boolean open) {
        if (outlet == null) {
            return null;
        }
        LocalTime time = open ? outlet.windowOpen() : outlet.windowClose();
        return time == null ? null : time.format(WALL_CLOCK);
    }
}