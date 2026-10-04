package com.synapse.waypoint.driver.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.synapse.waypoint.driver.entity.ProblemStatus;
import com.synapse.waypoint.driver.entity.VehicleProblemKind;

/**
 * A problem a driver reported from the cab.
 *
 * <p>canDrive is the field that matters most: it decides whether dispatch re-plans the rest
 * of the run, and unitsOnBoard is what that re-plan moves - the cases stranded on this vehicle.
 */
public record VehicleProblemDto(
        String id,
        String vehicleId,
        String tripId,
        VehicleProblemKind kind,
        boolean canDrive,
        BigDecimal fridgeTempC,
        Integer unitsOnBoard,
        String note,
        ProblemStatus status,
        String reply,
        Instant reportedAt) {
}