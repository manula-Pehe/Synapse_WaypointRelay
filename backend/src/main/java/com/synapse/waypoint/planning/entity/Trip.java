package com.synapse.waypoint.planning.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.synapse.waypoint.planning.domain.TripWindowType;

/** One vehicle leaving the depot for one brand and district (table {@code trips}, V10). */
@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "plan_id", nullable = false, length = 40)
    private String planId;

    @Column(name = "vehicle_id", nullable = false, length = 10)
    private String vehicleId;

    @Column(name = "trip_no", nullable = false)
    private short tripNo;

    @Column(nullable = false, length = 10)
    private String brand;

    @Column(nullable = false, length = 40)
    private String district;

    @Enumerated(EnumType.STRING)
    @Column(name = "window_type", nullable = false, length = 10)
    private TripWindowType windowType;

    @Column(name = "depart_at", nullable = false)
    private Instant departAt;

    @Column(nullable = false)
    private int minutes;

    @Column(name = "weight_kg", nullable = false)
    private BigDecimal weightKg;

    @Column(name = "volume_m3", nullable = false)
    private BigDecimal volumeM3;

    @Column(nullable = false)
    private BigDecimal km;

    protected Trip() {
        // for JPA
    }

    public Trip(String id, String planId, String vehicleId, int tripNo, String brand, String district,
            TripWindowType windowType, Instant departAt, int minutes, BigDecimal weightKg, BigDecimal volumeM3,
            BigDecimal km) {
        this.id = id;
        this.planId = planId;
        this.vehicleId = vehicleId;
        this.tripNo = (short) tripNo;
        this.brand = brand;
        this.district = district;
        this.windowType = windowType;
        this.departAt = departAt;
        this.minutes = minutes;
        this.weightKg = weightKg;
        this.volumeM3 = volumeM3;
        this.km = km;
    }

    public String getId() {
        return id;
    }

    public String getPlanId() {
        return planId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public int getTripNo() {
        return tripNo;
    }

    public String getBrand() {
        return brand;
    }

    public String getDistrict() {
        return district;
    }

    public TripWindowType getWindowType() {
        return windowType;
    }

    public Instant getDepartAt() {
        return departAt;
    }

    public int getMinutes() {
        return minutes;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public BigDecimal getVolumeM3() {
        return volumeM3;
    }

    public BigDecimal getKm() {
        return km;
    }
}
