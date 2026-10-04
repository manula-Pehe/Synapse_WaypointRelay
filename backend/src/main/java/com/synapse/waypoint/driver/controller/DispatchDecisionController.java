package com.synapse.waypoint.driver.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.common.dto.ListResponse;
import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.driver.dto.ConflictDto;
import com.synapse.waypoint.driver.dto.DeliveryDto;
import com.synapse.waypoint.driver.entity.ConflictResolution;
import com.synapse.waypoint.driver.entity.FailedDeliveryDecision;
import com.synapse.waypoint.driver.service.DispatchDecisionService;

/**
 * The dispatcher's decisions on what drivers did - docs/api.md §8.
 *
 * F8 (D8) settles a clash between an offline delivery and a board edit; F9 (D6f) decides what happens
 * to a delivery that failed. Both are here because both end with the driver's own record carrying
 * the answer, which is what the driver reads back on their phone.
 */
@RestController
@RequestMapping("/api/dispatch")
class DispatchDecisionController {

    private final DispatchDecisionService decisions;
    private final DemoClock clock;

    DispatchDecisionController(DispatchDecisionService decisions, DemoClock clock) {
        this.decisions = decisions;
        this.clock = clock;
    }

    /** D8 - the clashes waiting on a decision card. */
    @GetMapping("/conflicts")
    ListResponse<ConflictDto> conflicts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate) {
        return ListResponse.of(decisions.openConflicts(runDate != null ? runDate : clock.runDate()));
    }

    /**
     * D8 - settles one clash.
     *
     * {@code keepField} is the default and the expected answer: the driver handed the goods over and
     * a board edit does not undo that, so the witnessed delivery stands.
     */
    @PostMapping("/conflicts/{id}/resolve")
    ConflictDto resolve(@PathVariable String id, @RequestBody ResolveRequest request) {
        return decisions.resolveConflict(id, resolutionOf(request.keepField()));
    }

    /** D6f - the failed deliveries still needing a decision. */
    @GetMapping("/failed")
    ListResponse<DeliveryDto> failed(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate) {
        return ListResponse.of(decisions.failedDeliveries(runDate != null ? runDate : clock.runDate()));
    }

    /** D6f - replan tomorrow, try later today, or cancel. */
    @PostMapping("/failed/{id}/decide")
    DeliveryDto decide(@PathVariable String id, @RequestBody DecideRequest request) {
        return decisions.decideFailedDelivery(id, decisionOf(request.decision()));
    }

    /** The dispatcher's call on a clash; absent means keep the field record. */
    record ResolveRequest(Boolean keepField) {
    }

    /** The dispatcher's call on a failed delivery. */
    record DecideRequest(String decision) {
    }

    private ConflictResolution resolutionOf(Boolean keepField) {
        if (keepField == null) {
            return ConflictResolution.KEEP_FIELD;
        }
        return keepField ? ConflictResolution.KEEP_FIELD : ConflictResolution.OVERRIDE;
    }

    private FailedDeliveryDecision decisionOf(String decision) {
        try {
            return FailedDeliveryDecision.valueOf(decision == null ? "" : decision.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException(ErrorCode.VALIDATION,
                    "decision must be REPLAN_TOMORROW, TRY_LATER_TODAY or CANCEL");
        }
    }
}