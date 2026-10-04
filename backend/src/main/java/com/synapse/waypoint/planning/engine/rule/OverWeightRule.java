package com.synapse.waypoint.planning.engine.rule;

import java.util.Optional;

import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.engine.TripDraft;
import com.synapse.waypoint.planning.engine.VehicleDay;

/** A trip's total weight must fit the vehicle. */
public class OverWeightRule extends TripRule {

    @Override
    public RuleCode code() {
        return RuleCode.OVER_WEIGHT;
    }

    @Override
    protected Optional<String> problemWith(VehicleDay day, TripDraft trip) {
        boolean tooHeavy = trip.weightKg().compareTo(day.vehicle().weightCapKg()) > 0;
        return tooHeavy
                ? Optional.of("The load of " + trip.weightKg().stripTrailingZeros().toPlainString() + " kg is over the "
                        + day.vehicle().weightCapKg().stripTrailingZeros().toPlainString() + " kg limit of "
                        + day.vehicle().id() + ".")
                : Optional.empty();
    }
}
