package com.synapse.waypoint.core.reference.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A delivery vehicle with its capacity and fuel figures (table {@code vehicles}, V1). */
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @Column(length = 10)
    private String id;

    @Column(nullable = false, length = 10)
    private String type;

    @Column(nullable = false, length = 10)
    private String temp;

    @Column(name = "weight_cap_kg", nullable = false)
    private BigDecimal weightCapKg;

    @Column(name = "volume_cap_m3", nullable = false)
    private BigDecimal volumeCapM3;

    @Column(name = "fuel_type", nullable = false, length = 20)
    private String fuelType;

    @Column(name = "km_per_l", nullable = false)
    private BigDecimal kmPerLitre;

    @Column(name = "weekly_fuel_quota_l", nullable = false)
    private BigDecimal weeklyFuelQuotaLitres;

    @Column(nullable = false, length = 20)
    private String depot;

    protected Vehicle() {
        // for JPA
    }

    public Vehicle(String id, String type, String temp, BigDecimal weightCapKg, BigDecimal volumeCapM3,
            String fuelType, BigDecimal kmPerLitre, BigDecimal weeklyFuelQuotaLitres, String depot) {
        this.id = id;
        this.type = type;
        this.temp = temp;
        this.weightCapKg = weightCapKg;
        this.volumeCapM3 = volumeCapM3;
        this.fuelType = fuelType;
        this.kmPerLitre = kmPerLitre;
        this.weeklyFuelQuotaLitres = weeklyFuelQuotaLitres;
        this.depot = depot;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTemp() {
        return temp;
    }

    public BigDecimal getWeightCapKg() {
        return weightCapKg;
    }

    public BigDecimal getVolumeCapM3() {
        return volumeCapM3;
    }

    public String getFuelType() {
        return fuelType;
    }

    public BigDecimal getKmPerLitre() {
        return kmPerLitre;
    }

    public BigDecimal getWeeklyFuelQuotaLitres() {
        return weeklyFuelQuotaLitres;
    }

    public String getDepot() {
        return depot;
    }
}
