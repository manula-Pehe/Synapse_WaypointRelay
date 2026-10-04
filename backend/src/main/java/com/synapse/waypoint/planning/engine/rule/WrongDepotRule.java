package com.synapse.waypoint.planning.engine.rule;

import java.util.Optional;

import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.engine.StopCandidate;
import com.synapse.waypoint.planning.engine.TripDraft;
import com.synapse.waypoint.planning.engine.VehicleDay;

/** A vehicle serves only outlets of its home depot. */
public class WrongDepotRule extends TripRule {

    @Override
    public RuleCode code() {
        return RuleCode.WRONG_DEPOT;
    }

    @Override
    protected Optional<String> problemWith(VehicleDay day, TripDraft trip) {
        return trip.stops().stream()
                .filter(stop -> !stop.outlet().depot().equalsIgnoreCase(day.vehicle().depot()))
                .findFirst()
                .map(stop -> message(day, stop));
    }

    private String message(VehicleDay day, StopCandidate stop) {
        return "Vehicle " + day.vehicle().id() + " belongs to " + day.vehicle().depot() + " and cannot serve outlet "
                + stop.outlet().id() + " of " + stop.outlet().depot() + ".";
    }
}
