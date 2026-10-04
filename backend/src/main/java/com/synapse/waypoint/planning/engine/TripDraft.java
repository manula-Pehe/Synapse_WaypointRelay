package com.synapse.waypoint.planning.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.synapse.waypoint.planning.domain.Brand;
import com.synapse.waypoint.planning.domain.TripWindowType;

/** A proposed trip: its stops in delivery order. Brand and district come from the first stop. */
public record TripDraft(List<StopCandidate> stops) {

    public TripDraft {
        if (stops.isEmpty()) {
            throw new IllegalArgumentException("A trip needs at least one stop");
        }
        stops = List.copyOf(stops);
    }

    public Brand brand() {
        return stops.get(0).order().brand();
    }

    public String district() {
        return stops.get(0).outlet().district();
    }

    public TripWindowType windowType() {
        return brand().windowType();
    }

    public BigDecimal weightKg() {
        return stops.stream().map(stop -> stop.order().weightKg()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal volumeM3() {
        return stops.stream().map(stop -> stop.order().volumeM3()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public TripDraft withStops(List<StopCandidate> newStops) {
        return new TripDraft(newStops);
    }

    public TripDraft withStop(StopCandidate stop) {
        List<StopCandidate> extended = new ArrayList<>(stops);
        extended.add(stop);
        return new TripDraft(extended);
    }
}
