package com.synapse.waypoint.planning.service;

import java.time.LocalDate;
import java.util.List;

import com.synapse.waypoint.planning.dto.DeferralDto;
import com.synapse.waypoint.planning.dto.PlanDto;
import com.synapse.waypoint.planning.dto.PlanReadinessDto;

/**
 * The dispatcher's plan commands and reads (docs/api.md §6). Other modules read published plans
 * through {@link PlanQueryService}, not this interface.
 */
public interface PlanService {

    /** What is still missing before a plan can be created (Dp0). */
    PlanReadinessDto readiness(LocalDate runDate, String depot);

    /**
     * Plans the run's confirmed orders. Refused with ORDERS_NOT_CLOSED before the cut-off and with
     * PLAN_LOCKED once a plan is published; an existing draft is replaced.
     */
    PlanDto create(LocalDate runDate, String depot);

    /** The latest plan of the run and depot; NOT_FOUND when none exists. */
    PlanDto latest(LocalDate runDate, String depot);

    PlanDto get(String planId);

    List<DeferralDto> deferrals(String planId);

    /** Makes the draft the plan the depot runs on; PLAN_LOCKED when it is already published. */
    PlanDto publish(String planId);
}
