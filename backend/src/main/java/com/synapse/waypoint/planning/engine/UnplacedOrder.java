package com.synapse.waypoint.planning.engine;

import java.util.Optional;

import com.synapse.waypoint.planning.engine.input.OrderInput;
import com.synapse.waypoint.planning.engine.rule.RuleViolation;

/** An order the allocator could not place, with the rule that blocked its last attempt (if it tried any). */
public record UnplacedOrder(OrderInput order, Optional<RuleViolation> lastBlock) {
}
