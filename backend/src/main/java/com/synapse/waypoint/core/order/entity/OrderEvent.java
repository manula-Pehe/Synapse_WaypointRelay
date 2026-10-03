package com.synapse.waypoint.core.order.entity;

import java.time.Instant;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One row of an order's history: who did what and when (table {@code order_events}, V1). */
@Entity
@Table(name = "order_events")
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 40)
    private String orderId;

    @Column(name = "at", nullable = false)
    private Instant at;

    /** Null when the system or a timed job made the change. */
    @Column(name = "actor_user_id", length = 40)
    private String actorUserId;

    @Column(nullable = false, length = 40)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20)
    private OrderStatus toStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> details;

    protected OrderEvent() {
        // for JPA
    }

    public OrderEvent(String orderId, Instant at, String actorUserId, String type, OrderStatus fromStatus,
            OrderStatus toStatus, Map<String, Object> details) {
        this.orderId = orderId;
        this.at = at;
        this.actorUserId = actorUserId;
        this.type = type;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.details = Map.copyOf(details);
    }

    public Long getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public Instant getAt() {
        return at;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public String getType() {
        return type;
    }

    public OrderStatus getFromStatus() {
        return fromStatus;
    }

    public OrderStatus getToStatus() {
        return toStatus;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
