package com.synapse.waypoint.core.reference.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Distances and free-flow times from a depot to a district (table {@code district_travel}, V1). */
@Entity
@Table(name = "district_travel")
public class DistrictTravel {

    @EmbeddedId
    private DistrictTravelId id;

    @Column(name = "road_class", nullable = false, length = 20)
    private String roadClass;

    @Column(name = "free_flow_kmh", nullable = false)
    private BigDecimal freeFlowKmh;

    @Column(name = "depot_to_district_km", nullable = false)
    private BigDecimal depotToDistrictKm;

    @Column(name = "outbound_min", nullable = false)
    private int outboundMinutes;

    @Column(name = "inter_stop_km", nullable = false)
    private BigDecimal interStopKm;

    @Column(name = "inter_stop_min", nullable = false)
    private int interStopMinutes;

    protected DistrictTravel() {
        // for JPA
    }

    public DistrictTravel(DistrictTravelId id, String roadClass, BigDecimal freeFlowKmh, BigDecimal depotToDistrictKm,
            int outboundMinutes, BigDecimal interStopKm, int interStopMinutes) {
        this.id = id;
        this.roadClass = roadClass;
        this.freeFlowKmh = freeFlowKmh;
        this.depotToDistrictKm = depotToDistrictKm;
        this.outboundMinutes = outboundMinutes;
        this.interStopKm = interStopKm;
        this.interStopMinutes = interStopMinutes;
    }

    public DistrictTravelId getId() {
        return id;
    }

    public String getRoadClass() {
        return roadClass;
    }

    public BigDecimal getFreeFlowKmh() {
        return freeFlowKmh;
    }

    public BigDecimal getDepotToDistrictKm() {
        return depotToDistrictKm;
    }

    public int getOutboundMinutes() {
        return outboundMinutes;
    }

    public BigDecimal getInterStopKm() {
        return interStopKm;
    }

    public int getInterStopMinutes() {
        return interStopMinutes;
    }
}
