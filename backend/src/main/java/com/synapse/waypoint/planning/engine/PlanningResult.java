package com.synapse.waypoint.planning.engine;

import java.util.List;

import com.synapse.waypoint.planning.engine.rule.RuleViolation;

/** What the engine returns: trips, deferrals and the summary, plus any hard-rule violations the final check found and soft warnings (late arrivals). */
public record PlanningResult(List<PlannedTrip> trips, List<PlannedDeferral> deferrals, PlanSummary summary,
        List<RuleViolation> violations, List<RuleViolation> warnings) {

    public PlanningResult {
        trips = List.copyOf(trips);
        deferrals = List.copyOf(deferrals);
        violations = List.copyOf(violations);
        warnings = List.copyOf(warnings);
    }
}
