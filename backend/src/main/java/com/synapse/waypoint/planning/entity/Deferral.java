package com.synapse.waypoint.planning.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.synapse.waypoint.planning.domain.DeferralKind;
import com.synapse.waypoint.planning.domain.RuleCode;

/** An order that cannot go on this run, with the rule and reason (table {@code deferrals}, V10). */
@Entity
@Table(name = "deferrals")
public class Deferral {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "plan_id", nullable = false, length = 40)
    private String planId;

    @Column(name = "order_id", nullable = false, length = 40)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeferralKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RuleCode rule;

    @Column(nullable = false)
    private String reason;

    @Column(name = "priority_score", nullable = false)
    private int priorityScore;

    @Column(name = "days_waited", nullable = false)
    private int daysWaited;

    @Column(name = "new_date", nullable = false)
    private LocalDate newDate;

    @Column(name = "needs_decision", nullable = false)
    private boolean needsDecision;

    protected Deferral() {
        // for JPA
    }

    public Deferral(String id, String planId, String orderId, DeferralKind kind, RuleCode rule, String reason,
            int priorityScore, int daysWaited, LocalDate newDate, boolean needsDecision) {
        this.id = id;
        this.planId = planId;
        this.orderId = orderId;
        this.kind = kind;
        this.rule = rule;
        this.reason = reason;
        this.priorityScore = priorityScore;
        this.daysWaited = daysWaited;
        this.newDate = newDate;
        this.needsDecision = needsDecision;
    }

    public String getId() {
        return id;
    }

    public String getPlanId() {
        return planId;
    }

    public String getOrderId() {
        return orderId;
    }

    public DeferralKind getKind() {
        return kind;
    }

    public RuleCode getRule() {
        return rule;
    }

    public String getReason() {
        return reason;
    }

    public int getPriorityScore() {
        return priorityScore;
    }

    public int getDaysWaited() {
        return daysWaited;
    }

    public LocalDate getNewDate() {
        return newDate;
    }

    public boolean isNeedsDecision() {
        return needsDecision;
    }
}
