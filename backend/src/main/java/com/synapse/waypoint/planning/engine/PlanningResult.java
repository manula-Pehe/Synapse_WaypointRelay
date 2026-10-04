package com.synapse.waypoint.planning.engine;

import java.util.List;

import com.synapse.waypoint.planning.engine.rule.RuleViolation;

/** What the engine returns: trips, deferrals and the summary, plus any violations the final check found. */
public record PlanningResult(List<PlannedTrip> trips, List<PlannedDeferral> deferrals, PlanSummary summary,
        List<RuleViolation> violations) {

    public PlanningResult {
        trips = List.copyOf(trips);
        deferrals = List.copyOf(deferrals);
        violations = List.copyOf(violations);
    }
}
