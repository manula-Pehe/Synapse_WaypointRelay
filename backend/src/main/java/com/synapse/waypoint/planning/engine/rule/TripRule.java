package com.synapse.waypoint.planning.engine.rule;

import java.util.Optional;

import com.synapse.waypoint.planning.engine.TripDraft;
import com.synapse.waypoint.planning.engine.VehicleDay;

/** A rule that judges each trip on its own; reports the first trip that breaks it. */
abstract class TripRule implements PlanningRule {

    @Override
    public Optional<RuleViolation> check(VehicleDay day) {
        for (TripDraft trip : day.trips()) {
            Optional<String> problem = problemWith(day, trip);
            if (problem.isPresent()) {
                return Optional.of(new RuleViolation(code(), problem.get()));
            }
        }
        return Optional.empty();
    }

    /** The reason the trip breaks the rule, or empty when it is fine. */
    protected abstract Optional<String> problemWith(VehicleDay day, TripDraft trip);
}
