package com.synapse.waypoint.planning.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.synapse.waypoint.planning.domain.TripWindowType;
import com.synapse.waypoint.planning.engine.input.PlanningInput;
import com.synapse.waypoint.planning.engine.input.TravelInput;
import com.synapse.waypoint.planning.engine.input.VehicleInput;

/**
 * The booklet formulas. Trip minutes = outbound + inter-stop × (stops − 1) + Σ service allowance.
 * Trip km = 2 × depot-to-district km + inter-stop km × (stops − 1). Litres = km ÷ km per litre.
 * A truck that reaches an outlet before its window opens waits there, which delays the stops after it;
 * the booklet's trip minutes (used for the time budget) do not include that waiting.
 */
public class TripCalculator {

    private static final int LITRE_SCALE = 4;
    private static final int OUT_AND_BACK = 2;

    private final PlanningInput input;

    public TripCalculator(PlanningInput input) {
        this.input = input;
    }

    public int minutes(TripDraft trip) {
        TravelInput travel = input.travel(trip.district());
        int betweenStops = travel.interStopMinutes() * (trip.stops().size() - 1);
        return travel.outboundMinutes() + betweenStops + trip.stops().stream().mapToInt(this::serviceMinutes).sum();
    }

    public BigDecimal km(TripDraft trip) {
        TravelInput travel = input.travel(trip.district());
        BigDecimal betweenStops = travel.interStopKm().multiply(BigDecimal.valueOf(trip.stops().size() - 1L));
        return travel.depotToDistrictKm().multiply(BigDecimal.valueOf(OUT_AND_BACK)).add(betweenStops);
    }

    public BigDecimal litres(TripDraft trip, VehicleInput vehicle) {
        return km(trip).divide(vehicle.kmPerLitre(), LITRE_SCALE, RoundingMode.HALF_UP);
    }

    /** Litres the vehicle burns on all its trips of the day. */
    public BigDecimal litres(VehicleDay day) {
        return day.trips().stream()
                .map(trip -> litres(trip, day.vehicle()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Minutes the vehicle spends in one window type; the budget applies to this total. */
    public int minutesInWindow(VehicleDay day, TripWindowType windowType) {
        return day.trips().stream().filter(trip -> trip.windowType() == windowType).mapToInt(this::minutes).sum();
    }

    /**
     * A trip leaves when the vehicle's previous trips of the same window type have finished, the first
     * one at the window's first departure time.
     */
    public TripTiming timing(VehicleDay day, int tripIndex) {
        TripDraft trip = day.trips().get(tripIndex);
        int earlierMinutes = day.trips().subList(0, tripIndex).stream()
                .filter(earlier -> earlier.windowType() == trip.windowType())
                .mapToInt(this::minutes)
                .sum();
        LocalTime departure = trip.windowType().firstDeparture().plusMinutes(earlierMinutes);
        return new TripTiming(departure, minutes(trip), arrivals(trip, departure));
    }

    private List<LocalTime> arrivals(TripDraft trip, LocalTime departure) {
        TravelInput travel = input.travel(trip.district());
        List<LocalTime> arrivals = new ArrayList<>();
        LocalTime clock = departure.plusMinutes(travel.outboundMinutes());
        for (int index = 0; index < trip.stops().size(); index++) {
            if (index > 0) {
                clock = clock.plusMinutes(serviceMinutes(trip.stops().get(index - 1)) + travel.interStopMinutes());
            }
            clock = laterOf(clock, trip.stops().get(index).window().open());
            arrivals.add(clock);
        }
        return arrivals;
    }

    private static LocalTime laterOf(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    private int serviceMinutes(StopCandidate stop) {
        return input.serviceMinutes(stop.order().brand(), stop.outlet().dockType());
    }
}
