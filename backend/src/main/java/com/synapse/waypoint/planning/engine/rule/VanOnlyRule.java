package com.synapse.waypoint.planning.engine.rule;

import java.util.Optional;

import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.domain.VehicleType;
import com.synapse.waypoint.planning.engine.StopCandidate;
import com.synapse.waypoint.planning.engine.TripDraft;
import com.synapse.waypoint.planning.engine.VehicleDay;

/** Outlets that trucks cannot reach are served by vans only. */
public class VanOnlyRule extends TripRule {

    @Override
    public RuleCode code() {
        return RuleCode.VAN_ONLY;
    }

    @Override
    protected Optional<String> problemWith(VehicleDay day, TripDraft trip) {
        if (day.vehicle().type() == VehicleType.VAN) {
            return Optional.empty();
        }
        return trip.stops().stream()
                .filter(stop -> stop.outlet().isVanOnly())
                .findFirst()
                .map(this::message);
    }

    private String message(StopCandidate stop) {
        return "Outlet " + stop.outlet().id() + " can only be reached by a van.";
    }
}
