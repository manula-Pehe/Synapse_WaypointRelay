package com.synapse.waypoint.driver.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Something wrong with the vehicle, reported from the cab vehicle_problems, V20;
 * screens R9 and R9ok).
 *
 * <p>Written from the phone's outbox, so it carries the clientId that made it - a driver who
 * reports the same flat tyre twice while offline still produces one report. reply holds the
 * dispatcher's instruction, which the driver reads on the same screen instead of phoning.
 *
 * <p>unitsOnBoard is what the breakdown re-plan works from (D6b): how many cases are stranded on this
 * vehicle right now. Absent when there is no load - a breakdown on the way back to the depot.
 */
@Entity
@Table(name = "vehicle_problems")
public class VehicleProblem {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "vehicle_id", nullable = false, length = 10)
    private String vehicleId;

    @Column(name = "trip_id", length = 40)
    private String tripId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleProblemKind kind;

    @Column(name = "can_drive", nullable = false)
    private boolean canDrive;

    @Column(name = "fridge_temp_c", precision = 4, scale = 1)
    private BigDecimal fridgeTempC;

    @Column(name = "units_on_board")
    private Integer unitsOnBoard;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProblemStatus status = ProblemStatus.OPEN;

    /** The dispatcher's answer, shown on R9ok. */
    @Column(columnDefinition = "TEXT")
    private String reply;

    @Column(name = "reported_by", length = 40)
    private String reportedBy;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;

    @Column(name = "replied_by", length = 40)
    private String repliedBy;

    @Column(name = "replied_at")
    private Instant repliedAt;

    @Column(name = "client_id", nullable = false, length = 40, unique = true)
    private String clientId;

    protected VehicleProblem() {
        // for JPA
    }

    private VehicleProblem(ReportedProblem reported) {
        this.id = reported.id();
        this.vehicleId = reported.vehicleId();
        this.tripId = reported.tripId();
        this.kind = reported.kind();
        this.canDrive = reported.canDrive();
        this.fridgeTempC = reported.fridgeTempC();
        this.unitsOnBoard = reported.unitsOnBoard();
        this.note = reported.note();
        this.reportedBy = reported.reportedBy();
        this.reportedAt = reported.reportedAt();
        this.clientId = reported.clientId();
    }

    public static VehicleProblem report(ReportedProblem reported) {
        return new VehicleProblem(reported);
    }

    /** Dispatch answers the driver in writing (R9ok), so nobody has to phone while driving. */
    public void reply(String text, String dispatcherId, Instant now) {
        this.reply = text;
        this.repliedBy = dispatcherId;
        this.repliedAt = now;
    }

    public void resolve(Instant now) {
        this.status = ProblemStatus.RESOLVED;
        if (this.repliedAt == null) {
            this.repliedAt = now;
        }
    }

    public boolean isOpen() {
        return status == ProblemStatus.OPEN;
    }

    public String getId() {
        return id;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getTripId() {
        return tripId;
    }

    public VehicleProblemKind getKind() {
        return kind;
    }

    public boolean isCanDrive() {
        return canDrive;
    }

    public BigDecimal getFridgeTempC() {
        return fridgeTempC;
    }

    public String getNote() {
        return note;
    }

    /** How many cases are stranded on this vehicle, or null when there is no load. */
    public Integer getUnitsOnBoard() {
        return unitsOnBoard;
    }

    public ProblemStatus getStatus() {
        return status;
    }

    public String getReply() {
        return reply;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public String getRepliedBy() {
        return repliedBy;
    }

    public Instant getRepliedAt() {
        return repliedAt;
    }

    public String getClientId() {
        return clientId;
    }

    /** The values a queued {@code VEHICLE_PROBLEM} carries. */
    public record ReportedProblem(String id, String vehicleId, String tripId, VehicleProblemKind kind,
            boolean canDrive, BigDecimal fridgeTempC, Integer unitsOnBoard, String note,
            String reportedBy, Instant reportedAt, String clientId) {
    }
}