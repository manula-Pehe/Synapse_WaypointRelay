package com.synapse.waypoint.planning.controller;

import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.common.dto.ListResponse;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.planning.dto.CreatePlanRequest;
import com.synapse.waypoint.planning.dto.DeferralDto;
import com.synapse.waypoint.planning.dto.PlanDto;
import com.synapse.waypoint.planning.dto.PlanReadinessDto;
import com.synapse.waypoint.planning.service.PlanService;

/** Dispatcher plan endpoints - docs/api.md §6 (Dp0–Dp3, D3, D4, D3p). Dispatcher-only by path rule. */
@RestController
@RequestMapping("/api/dispatch/plans")
class DispatchPlanController {

    private final PlanService plans;
    private final DemoClock clock;

    DispatchPlanController(PlanService plans, DemoClock clock) {
        this.plans = plans;
        this.clock = clock;
    }

    /** {@code runDate} defaults to the current run date; {@code depot} to the dispatcher's own depot. */
    @GetMapping("/readiness")
    PlanReadinessDto readiness(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate,
            @RequestParam(required = false) String depot) {
        return plans.readiness(runDateOrCurrent(runDate), depot);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PlanDto create(@Valid @RequestBody CreatePlanRequest request) {
        return plans.create(request.runDate(), request.depot());
    }

    @GetMapping
    PlanDto latest(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate runDate,
            @RequestParam(required = false) String depot) {
        return plans.latest(runDateOrCurrent(runDate), depot);
    }

    @GetMapping("/{id}")
    PlanDto get(@PathVariable String id) {
        return plans.get(id);
    }

    @GetMapping("/{id}/deferrals")
    ListResponse<DeferralDto> deferrals(@PathVariable String id) {
        return ListResponse.of(plans.deferrals(id));
    }

    @PostMapping("/{id}/publish")
    PlanDto publish(@PathVariable String id) {
        return plans.publish(id);
    }

    private LocalDate runDateOrCurrent(LocalDate runDate) {
        return runDate != null ? runDate : clock.runDate();
    }
}
