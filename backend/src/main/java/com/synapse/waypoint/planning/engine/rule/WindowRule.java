package com.synapse.waypoint.planning.engine.rule;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.engine.StopCandidate;
import com.synapse.waypoint.planning.engine.TripCalculator;
import com.synapse.waypoint.planning.engine.TripDraft;
import com.synapse.waypoint.planning.engine.VehicleDay;

/**
 * Each stop's predicted arrival should fall inside the outlet's window (the mall window for mall outlets).
 * A soft rule: used for warnings, not for accepting or rejecting a plan.
 */
public class WindowRule implements PlanningRule {

    private final TripCalculator calculator;

    public WindowRule(TripCalculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public RuleCode code() {
        return RuleCode.WINDOW;
    }

    @Override
    public Optional<RuleViolation> check(VehicleDay day) {
        for (int tripIndex = 0; tripIndex < day.trips().size(); tripIndex++) {
            TripDraft trip = day.trips().get(tripIndex);
            List<LocalTime> arrivals = calculator.timing(day, tripIndex).arrivals();
            for (int stopIndex = 0; stopIndex < arrivals.size(); stopIndex++) {
                StopCandidate stop = trip.stops().get(stopIndex);
                if (!stop.window().contains(arrivals.get(stopIndex))) {
                    return Optional.of(new RuleViolation(code(), message(stop, arrivals.get(stopIndex))));
                }
            }
        }
        return Optional.empty();
    }

    private String message(StopCandidate stop, LocalTime arrival) {
        return "Arrival at " + stop.outlet().id() + " would be " + arrival + ", outside its window "
                + stop.window().open() + "–" + stop.window().close() + ".";
    }
}
