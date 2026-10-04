package com.synapse.waypoint.planning.engine.rule;

import java.util.Optional;

import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.engine.VehicleDay;

/** One feasibility rule, checked over everything proposed for a vehicle on the run date. */
public interface PlanningRule {

    RuleCode code();

    Optional<RuleViolation> check(VehicleDay day);
}
