package com.synapse.waypoint.planning.engine.rule;

import java.util.Optional;

import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.engine.TripDraft;
import com.synapse.waypoint.planning.engine.VehicleDay;

/** Chilled orders only travel on reefer vehicles. */
public class FridgeRequiredRule extends TripRule {

    @Override
    public RuleCode code() {
        return RuleCode.FRIDGE_REQUIRED;
    }

    @Override
    protected Optional<String> problemWith(VehicleDay day, TripDraft trip) {
        boolean chilledWithoutFridge = !day.vehicle().isReefer()
                && trip.stops().stream().anyMatch(stop -> stop.order().isChilled());
        return chilledWithoutFridge
                ? Optional.of("Chilled orders need a fridge vehicle, and " + day.vehicle().id() + " is not one.")
                : Optional.empty();
    }
}
