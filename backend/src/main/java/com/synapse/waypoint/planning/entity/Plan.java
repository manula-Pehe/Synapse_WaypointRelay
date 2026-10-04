package com.synapse.waypoint.planning.entity;

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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.synapse.waypoint.planning.domain.PlanStatus;

/** One version of the plan for a run date and depot (table {@code plans}, V10). */
@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(nullable = false, length = 20)
    private String depot;

    @Column(nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> summary;

    @Column(name = "parent_plan_id", length = 40)
    private String parentPlanId;

    @Column(name = "revise_reason")
    private String reviseReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 40)
    private String createdBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by", length = 40)
    private String publishedBy;

    @Version
    @Column(name = "version_lock")
    private Integer versionLock;

    protected Plan() {
        // for JPA
    }

    public Plan(String id, LocalDate runDate, String depot, int version, PlanStatus status,
            Map<String, Object> summary, String parentPlanId, String reviseReason, Instant createdAt,
            String createdBy) {
        this.id = id;
        this.runDate = runDate;
        this.depot = depot;
        this.version = version;
        this.status = status;
        this.summary = Map.copyOf(summary);
        this.parentPlanId = parentPlanId;
        this.reviseReason = reviseReason;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public String getId() {
        return id;
    }

    public LocalDate getRunDate() {
        return runDate;
    }

    public String getDepot() {
        return depot;
    }

    public int getVersion() {
        return version;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public Map<String, Object> getSummary() {
        return summary;
    }

    public String getParentPlanId() {
        return parentPlanId;
    }

    public String getReviseReason() {
        return reviseReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getPublishedBy() {
        return publishedBy;
    }
}
