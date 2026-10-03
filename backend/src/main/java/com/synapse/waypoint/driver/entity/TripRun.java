package com.synapse.waypoint.driver.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The driver's side of a trip . Created when the driver accepts the
 * load at the depot ; lastSyncAt is what the live board shows as "offline" (D6).
 */
@Entity
@Table(name = "trip_runs")
public class TripRun {

    @Id
    @Column(name = "trip_id", length = 40)
    private String tripId;

    @Column(name = "driver_id", nullable = false, length = 40)
    private String driverId;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    protected TripRun() {
        // for JPA
    }

    private TripRun(String tripId, String driverId) {
        this.tripId = tripId;
        this.driverId = driverId;
    }

    public static TripRun accepted(String tripId, String driverId, Instant now) {
        TripRun run = new TripRun(tripId, driverId);
        run.acceptedAt = now;
        return run;
    }

    /** The driver tapped "Go to stop 1" (R2). */
    public void start(Instant now) {
        startedAt = now;
    }

    /** The trip is complete (R8); returned goods are handed back at the depot (R8r). */
    public void end(Instant now) {
        endedAt = now;
    }

    /** Called after every successful sync so the live board can tell an offline driver from a slow one. */
    public void syncedAt(Instant now) {
        lastSyncAt = now;
    }

    public String getTripId() {
        return tripId;
    }

    public String getDriverId() {
        return driverId;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }
}