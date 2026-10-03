package com.synapse.waypoint.core.order.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A store's delivery order (table {@code orders}, V1). Status changes are made only through
 * {@code OrderService}, so this class exposes no status setter.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(length = 40)
    private String id;

    @Column(nullable = false, length = 40)
    private String ref;

    @Column(name = "outlet_id", nullable = false, length = 10)
    private String outletId;

    @Column(nullable = false, length = 10)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(name = "temp_requirement", nullable = false, length = 10)
    private TemperatureRequirement temperatureRequirement;

    @Column(nullable = false)
    private int units;

    @Column(name = "weight_kg", nullable = false)
    private BigDecimal weightKg;

    @Column(name = "volume_m3", nullable = false)
    private BigDecimal volumeM3;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderSource source;

    @Column(name = "auto_confirm", nullable = false)
    private boolean autoConfirm;

    @Column(name = "days_since_last_served", nullable = false)
    private int daysSinceLastServed;

    @Column(name = "deferred_yesterday", nullable = false)
    private boolean deferredYesterday;

    @Column(name = "parent_order_id", length = 40)
    private String parentOrderId;

    @Column(name = "store_checked", nullable = false)
    private boolean storeChecked;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "confirmed_by", length = 40)
    private String confirmedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Null until first saved; Hibernate then writes 0 (the column is NOT NULL). */
    @Version
    private Integer version;

    protected Order() {
        // for JPA
    }

    public static Order create(NewOrder spec, Instant now) {
        Order order = new Order();
        order.id = spec.id();
        order.ref = spec.ref();
        order.outletId = spec.outletId();
        order.brand = spec.brand();
        order.temperatureRequirement = spec.temperatureRequirement();
        order.units = spec.units();
        order.weightKg = spec.weightKg();
        order.volumeM3 = spec.volumeM3();
        order.runDate = spec.runDate();
        order.status = spec.status();
        order.source = spec.source();
        order.autoConfirm = spec.autoConfirm();
        order.daysSinceLastServed = spec.daysSinceLastServed();
        order.deferredYesterday = spec.deferredYesterday();
        order.parentOrderId = spec.parentOrderId();
        order.storeChecked = spec.storeChecked();
        order.createdAt = now;
        order.updatedAt = now;
        return order;
    }

    public String getId() {
        return id;
    }

    public String getRef() {
        return ref;
    }

    public String getOutletId() {
        return outletId;
    }

    public String getBrand() {
        return brand;
    }

    public TemperatureRequirement getTemperatureRequirement() {
        return temperatureRequirement;
    }

    public int getUnits() {
        return units;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public BigDecimal getVolumeM3() {
        return volumeM3;
    }

    public LocalDate getRunDate() {
        return runDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public OrderSource getSource() {
        return source;
    }

    public boolean isAutoConfirm() {
        return autoConfirm;
    }

    public int getDaysSinceLastServed() {
        return daysSinceLastServed;
    }

    public boolean isDeferredYesterday() {
        return deferredYesterday;
    }

    public String getParentOrderId() {
        return parentOrderId;
    }

    public boolean isStoreChecked() {
        return storeChecked;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public String getConfirmedBy() {
        return confirmedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Integer getVersion() {
        return version;
    }
}
