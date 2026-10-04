package com.synapse.waypoint.planning.engine;

import java.time.LocalDate;

import com.synapse.waypoint.planning.domain.DeferralKind;
import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.engine.input.OrderInput;

/** An order left out of the plan, with the deciding rule, a plain-language reason and its new date. */
public record PlannedDeferral(OrderInput order, DeferralKind kind, RuleCode rule, String reason, int priorityScore,
        int daysWaited, LocalDate newDate, boolean needsDecision) {
}
