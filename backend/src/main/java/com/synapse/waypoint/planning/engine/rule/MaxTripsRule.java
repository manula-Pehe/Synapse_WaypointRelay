package com.synapse.waypoint.planning.engine.rule;

import java.util.Optional;

import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.engine.VehicleDay;

/** A vehicle makes at most two trips a day. */
public class MaxTripsRule implements PlanningRule {

    public static final int MAX_TRIPS_PER_VEHICLE = 2;

    @Override
    public RuleCode code() {
        return RuleCode.MAX_TRIPS;
    }

    @Override
    public Optional<RuleViolation> check(VehicleDay day) {
        if (day.trips().size() <= MAX_TRIPS_PER_VEHICLE) {
            return Optional.empty();
        }
        return Optional.of(new RuleViolation(code(),
                "Vehicle " + day.vehicle().id() + " can make at most " + MAX_TRIPS_PER_VEHICLE + " trips a day."));
    }
}
