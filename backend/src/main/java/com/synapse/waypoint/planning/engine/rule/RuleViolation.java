package com.synapse.waypoint.planning.engine.rule;

import com.synapse.waypoint.planning.domain.RuleCode;

/** A broken planning rule with a plain-language explanation. */
public record RuleViolation(RuleCode rule, String message) {
}
