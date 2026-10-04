package com.synapse.waypoint.planning.engine.rule;

import java.util.Optional;

import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.engine.TripDraft;
import com.synapse.waypoint.planning.engine.VehicleDay;

/** A trip's total volume must fit the vehicle. */
public class OverVolumeRule extends TripRule {

    @Override
    public RuleCode code() {
        return RuleCode.OVER_VOLUME;
    }

    @Override
    protected Optional<String> problemWith(VehicleDay day, TripDraft trip) {
        boolean tooBulky = trip.volumeM3().compareTo(day.vehicle().volumeCapM3()) > 0;
        return tooBulky
                ? Optional.of("The load of " + trip.volumeM3().stripTrailingZeros().toPlainString() + " m³ is over the "
                        + day.vehicle().volumeCapM3().stripTrailingZeros().toPlainString() + " m³ limit of "
                        + day.vehicle().id() + ".")
                : Optional.empty();
    }
}
