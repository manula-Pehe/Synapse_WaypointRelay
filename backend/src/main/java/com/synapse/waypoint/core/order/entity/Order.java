package com.synapse.waypoint.core.order.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.core.order.exception.InvalidStatusException;

/**
 * A store's delivery order (table {@code orders}, V1). Status changes are made only through
 * {@code OrderService}; this class enforces the lifecycle but has no free status setter.
 */
@Entity
@Table(name = "orders")
public class Order {

    private static final int WEIGHT_SCALE = 2;
    private static final int VOLUME_SCALE = 3;

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

    /** Moves to {@code target}, or throws {@link InvalidStatusException} if the lifecycle does not allow it. */
    public void changeStatus(OrderStatus target, Instant now) {
        if (!status.canMoveTo(target)) {
            throw InvalidStatusException.transition(status, target);
        }
        status = target;
        updatedAt = now;
    }

    public void confirm(String userId, Instant now) {
        changeStatus(OrderStatus.CONFIRMED, now);
        confirmedAt = now;
        confirmedBy = userId;
    }

    /** Confirms on the store's behalf at the cut-off: no user, and the order is marked as auto-confirmed. */
    public void confirmAutomatically(Instant now) {
        confirm(null, now);
        autoConfirm = true;
    }

    /** Changes a prepared or confirmed order before the cut-off; weight and volume follow in proportion. */
    public void editUnits(int newUnits, Instant now) {
        if (status != OrderStatus.PREPARED && status != OrderStatus.CONFIRMED) {
            throw InvalidStatusException.notEditable(status);
        }
        rescale(newUnits, now);
    }

    /** Shrinks a deferred order to fewer units; weight and volume follow in proportion. */
    public void resizeMoved(int newUnits, Instant now) {
        requireMoved("resized");
        if (newUnits < 1 || newUnits >= units) {
            throw new DomainException(ErrorCode.VALIDATION, "A deferred order can only be reduced.",
                    Map.of("units", "must be between 1 and " + (units - 1)));
        }
        rescale(newUnits, now);
    }

    /** Cancels a deferred order; only a MOVED order, whatever the cut-off. */
    public void cancelMoved(Instant now) {
        requireMoved("cancelled this way");
        changeStatus(OrderStatus.CANCELLED, now);
    }

    private void requireMoved(String action) {
        if (status != OrderStatus.MOVED) {
            throw InvalidStatusException.notMoved(status, action);
        }
    }

    private void rescale(int newUnits, Instant now) {
        if (units > 0) {
            weightKg = scaled(weightKg, newUnits, WEIGHT_SCALE);
            volumeM3 = scaled(volumeM3, newUnits, VOLUME_SCALE);
        }
        units = newUnits;
        updatedAt = now;
    }

    /** Defers the order to {@code newDate}; the order keeps its id. */
    public void moveTo(LocalDate newDate, Instant now) {
        changeStatus(OrderStatus.MOVED, now);
        runDate = newDate;
    }

    private BigDecimal scaled(BigDecimal total, int newUnits, int scale) {
        return total.multiply(BigDecimal.valueOf(newUnits)).divide(BigDecimal.valueOf(units), scale,
                RoundingMode.HALF_UP);
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
