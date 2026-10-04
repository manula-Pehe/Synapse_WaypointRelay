package com.synapse.waypoint.planning.dto;

import java.time.LocalDate;

import com.synapse.waypoint.planning.domain.DeferralKind;
import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.domain.StoreChoice;

/** An order left out of the plan, with the deciding rule and why (docs/api.md §6). */
public record DeferralDto(String id, String orderId, String orderRef, String outletId, DeferralKind kind,
        RuleCode rule, String reason, int priorityScore, int daysWaited, LocalDate newDate, boolean needsDecision,
        StoreChoice storeChoice) {
}
