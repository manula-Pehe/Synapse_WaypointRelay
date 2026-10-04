package com.synapse.waypoint.planning.dto;

import java.time.LocalDate;

import com.synapse.waypoint.planning.domain.DeferralKind;
import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.domain.StoreChoice;

/** A deferral as the store sees it (docs/api.md §6, Store-facing); {@code splitOffered} when no vehicle can carry it. */
public record StoreDeferralDto(String id, DeferralKind kind, RuleCode rule, String reason, LocalDate newDate,
        boolean needsDecision, StoreChoice storeChoice, boolean splitOffered) {

    public static StoreDeferralDto from(DeferralDto deferral) {
        return new StoreDeferralDto(deferral.id(), deferral.kind(), deferral.rule(), deferral.reason(),
                deferral.newDate(), deferral.needsDecision(), deferral.storeChoice(),
                deferral.kind() == DeferralKind.UNAVOIDABLE);
    }
}
