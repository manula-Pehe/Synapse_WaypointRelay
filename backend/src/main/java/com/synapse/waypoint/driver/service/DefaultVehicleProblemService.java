package com.synapse.waypoint.driver.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.driver.dto.ReportProblemRequest;
import com.synapse.waypoint.driver.dto.VehicleProblemDto;
import com.synapse.waypoint.driver.entity.ProblemStatus;
import com.synapse.waypoint.driver.entity.VehicleProblem;
import com.synapse.waypoint.driver.entity.VehicleProblemKind;
import com.synapse.waypoint.driver.repository.VehicleProblemRepository;

/** Stores the cab reports and dispatch's answers (docs/api.md §7, F10). */
@Service
@Transactional
class DefaultVehicleProblemService implements VehicleProblemService {

    private static final String ID_PREFIX = "vpr-";

    private final VehicleProblemRepository problems;
    private final CurrentUser currentUser;
    private final DemoClock clock;

    DefaultVehicleProblemService(VehicleProblemRepository problems, CurrentUser currentUser, DemoClock clock) {
        this.problems = problems;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    public VehicleProblemDto report(String userId, String vehicleId, ReportProblemRequest request) {
        if (vehicleId == null) {
            throw new DomainException(ErrorCode.FORBIDDEN, "this account has no vehicle to report about");
        }
        VehicleProblemKind kind = kindOf(request.kind());

        // The clientId makes a retried upload safe: a phone that lost the response sends the same
        // report again and gets the row it already has rather than a second breakdown (US-1.2).
        if (request.clientId() != null) {
            var already = problems.findAll().stream()
                    .filter(problem -> request.clientId().equals(problem.getClientId()))
                    .findFirst();
            if (already.isPresent()) {
                return toDto(already.get());
            }
        }

        VehicleProblem problem = problems.save(VehicleProblem.report(new VehicleProblem.ReportedProblem(
                ID_PREFIX + UUID.randomUUID(),
                vehicleId,
                request.tripId(),
                kind,
                // Absent means the driver could not say, and the safe reading is they cannot drive on.
                request.canDrive() == null || request.canDrive(),
                request.fridgeTempC() == null ? null : BigDecimal.valueOf(request.fridgeTempC()),
                request.unitsOnBoard(),
                request.note(),
                userId,
                clock.now(),
                request.clientId())));

        return toDto(problem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleProblemDto> forVehicle(String vehicleId) {
        return problems.findByVehicleIdOrderByReportedAtDesc(vehicleId).stream()
                .map(DefaultVehicleProblemService::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleProblemDto> open(LocalDate runDate) {
        Instant start = runDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = runDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return problems.findByStatusOrderByReportedAtDesc(ProblemStatus.OPEN).stream()
                .filter(problem -> !problem.getReportedAt().isBefore(start)
                        && problem.getReportedAt().isBefore(end))
                .map(DefaultVehicleProblemService::toDto)
                .toList();
    }

    @Override
    public VehicleProblemDto reply(String problemId, String text) {
        if (text == null || text.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION, "the driver needs something to read");
        }
        VehicleProblem problem = problems.findById(problemId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "no such vehicle problem"));
        problem.reply(text, currentUser.id(), clock.now());
        return toDto(problems.save(problem));
    }

    private VehicleProblemKind kindOf(String kind) {
        try {
            return VehicleProblemKind.valueOf(kind == null ? "" : kind.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException(ErrorCode.VALIDATION, "kind must be one of BREAKDOWN, FRIDGE_FAULT, ACCIDENT, TYRE, OTHER");
        }
    }

    private static VehicleProblemDto toDto(VehicleProblem problem) {
        return new VehicleProblemDto(
                problem.getId(),
                problem.getVehicleId(),
                problem.getTripId(),
                problem.getKind(),
                problem.isCanDrive(),
                problem.getFridgeTempC(),
                problem.getUnitsOnBoard(),
                problem.getNote(),
                problem.getStatus(),
                problem.getReply(),
                problem.getReportedAt());
    }
}