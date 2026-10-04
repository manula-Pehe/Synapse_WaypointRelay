package com.synapse.waypoint.planning.engine;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.synapse.waypoint.planning.engine.input.VehicleInput;

/** Turns the allocator's vehicle days into planned trips with arrival windows and loading order. */
class PlanAssembler {

    private final TripCalculator calculator;
    private final ArrivalEstimator estimator;

    PlanAssembler(TripCalculator calculator, ArrivalEstimator estimator) {
        this.calculator = calculator;
        this.estimator = estimator;
    }

    List<PlannedTrip> assemble(List<VehicleDay> days) {
        return days.stream()
                .sorted(Comparator.comparing(day -> day.vehicle().id()))
                .flatMap(day -> tripsOf(day).stream())
                .toList();
    }

    private List<PlannedTrip> tripsOf(VehicleDay day) {
        List<PlannedTrip> trips = new ArrayList<>();
        for (int tripIndex = 0; tripIndex < day.trips().size(); tripIndex++) {
            trips.add(plannedTrip(day, tripIndex));
        }
        return trips;
    }

    private PlannedTrip plannedTrip(VehicleDay day, int tripIndex) {
        VehicleInput vehicle = day.vehicle();
        TripDraft trip = day.trips().get(tripIndex);
        TripTiming timing = calculator.timing(day, tripIndex);
        return new PlannedTrip(vehicle.id(), tripIndex + 1, trip.brand(), trip.district(), trip.windowType(),
                timing.departure(), timing.minutes(), trip.weightKg(), trip.volumeM3(), calculator.km(trip),
                plannedStops(trip, timing));
    }

    private List<PlannedStop> plannedStops(TripDraft trip, TripTiming timing) {
        int stopCount = trip.stops().size();
        List<PlannedStop> stops = new ArrayList<>();
        for (int index = 0; index < stopCount; index++) {
            StopCandidate stop = trip.stops().get(index);
            LocalTime predicted = timing.arrivals().get(index);
            ArrivalWindow arrival = estimator.estimate(stop.window(), predicted);
            stops.add(new PlannedStop(stop.order(), index + 1, stopCount - index, arrival.from(), arrival.to(),
                    arrival.lateRisk()));
        }
        return stops;
    }
}
