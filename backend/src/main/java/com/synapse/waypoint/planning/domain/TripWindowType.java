package com.synapse.waypoint.planning.domain;

import java.time.LocalTime;

/** Which time budget a trip uses: pre-dawn Fresh (3:30–8:00 AM) or daytime Style and Tech. */
public enum TripWindowType {
    FRESH(LocalTime.of(3, 30), 270),
    DAYTIME(LocalTime.of(8, 0), 480);

    private final LocalTime firstDeparture;
    private final int budgetMinutes;

    TripWindowType(LocalTime firstDeparture, int budgetMinutes) {
        this.firstDeparture = firstDeparture;
        this.budgetMinutes = budgetMinutes;
    }

    /** When a vehicle's first trip of this type leaves the depot. */
    public LocalTime firstDeparture() {
        return firstDeparture;
    }

    /** Total trip minutes one vehicle may spend in this window. */
    public int budgetMinutes() {
        return budgetMinutes;
    }
}
