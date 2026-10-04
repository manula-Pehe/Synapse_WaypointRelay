package com.synapse.waypoint.driver.service;

import java.time.LocalDate;
import java.util.List;

import com.synapse.waypoint.driver.dto.ReportProblemRequest;
import com.synapse.waypoint.driver.dto.VehicleProblemDto;

/**
 * Vehicle problems from the cab and dispatch's answer to them (docs/api.md §7, F10).
 *
 * The report arrives twice by design: queued through the outbox when the driver has no signal, and
 * directly when they do. Both paths end here, so the offline one cannot drift from the online one.
 */
public interface VehicleProblemService {

    /** R9 - records what the driver reported. */
    VehicleProblemDto report(String userId, String vehicleId, ReportProblemRequest request);

    /** R9ok - the thread on this vehicle, so the driver can see whether dispatch has answered. */
    List<VehicleProblemDto> forVehicle(String vehicleId);

    /** What dispatch sees, newest first. */
    List<VehicleProblemDto> open(LocalDate runDate);

    /** R9ok - dispatch's written answer, shown on the driver's screen. */
    VehicleProblemDto reply(String problemId, String text);
}