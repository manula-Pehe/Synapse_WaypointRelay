package com.synapse.waypoint.planning.choice;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.planning.domain.DeferralKind;
import com.synapse.waypoint.planning.domain.StoreChoice;

/** The store asks dispatch to split an order no vehicle can carry. Only recorded; dispatch acts on it. */
@Component
class SplitEffect implements ChoiceEffect {

    @Override
    public StoreChoice choice() {
        return StoreChoice.SPLIT;
    }

    @Override
    public boolean takesUnits() {
        return true;
    }

    @Override
    public void validate(ChoiceContext context) {
        if (context.deferral().getKind() != DeferralKind.UNAVOIDABLE) {
            throw new DomainException(ErrorCode.VALIDATION, "A split is only offered for an order no vehicle can carry.",
                    Map.of("choice", StoreChoice.SPLIT.name()));
        }
        if (context.units() != null) {
            ChoiceUnits.requireSmallerThanOrdered(context.order(), context.units());
        }
    }

    @Override
    public void apply(ChoiceContext context) {
        // Only the request is recorded; the order changes when dispatch splits it.
    }
}
