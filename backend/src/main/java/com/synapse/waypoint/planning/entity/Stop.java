package com.synapse.waypoint.planning.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One outlet delivery within a trip (table {@code stops}, V10). */
@Entity
@Table(name = "stops")
public class Stop {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 40)
    private String tripId;

    @Column(name = "order_id", nullable = false, length = 40)
    private String orderId;

    @Column(nullable = false)
    private short seq;

    @Column(name = "load_seq", nullable = false)
    private short loadSeq;

    @Column(name = "arrive_from", nullable = false)
    private Instant arriveFrom;

    @Column(name = "arrive_to", nullable = false)
    private Instant arriveTo;

    @Column(name = "late_risk", nullable = false)
    private BigDecimal lateRisk;

    protected Stop() {
        // for JPA
    }

    public Stop(String id, String tripId, String orderId, int seq, int loadSeq, Instant arriveFrom,
            Instant arriveTo, BigDecimal lateRisk) {
        this.id = id;
        this.tripId = tripId;
        this.orderId = orderId;
        this.seq = (short) seq;
        this.loadSeq = (short) loadSeq;
        this.arriveFrom = arriveFrom;
        this.arriveTo = arriveTo;
        this.lateRisk = lateRisk;
    }

    public String getId() {
        return id;
    }

    public String getTripId() {
        return tripId;
    }

    public String getOrderId() {
        return orderId;
    }

    public int getSeq() {
        return seq;
    }

    public int getLoadSeq() {
        return loadSeq;
    }

    public Instant getArriveFrom() {
        return arriveFrom;
    }

    public Instant getArriveTo() {
        return arriveTo;
    }

    public BigDecimal getLateRisk() {
        return lateRisk;
    }
}
