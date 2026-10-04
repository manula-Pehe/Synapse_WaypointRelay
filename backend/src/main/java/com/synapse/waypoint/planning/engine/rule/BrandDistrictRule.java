package com.synapse.waypoint.planning.engine.rule;

import java.util.Optional;

import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.engine.TripDraft;
import com.synapse.waypoint.planning.engine.VehicleDay;

/** A trip carries one brand and one district. */
public class BrandDistrictRule extends TripRule {

    @Override
    public RuleCode code() {
        return RuleCode.BRAND_DISTRICT;
    }

    @Override
    protected Optional<String> problemWith(VehicleDay day, TripDraft trip) {
        boolean mixed = trip.stops().stream().anyMatch(stop ->
                stop.order().brand() != trip.brand() || !stop.outlet().district().equals(trip.district()));
        return mixed
                ? Optional.of("A trip can serve only one brand in one district.")
                : Optional.empty();
    }
}
