package com.synapse.waypoint.planning.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.planning.domain.PlanStatus;
import com.synapse.waypoint.planning.dto.PlanSummaryDto;
import com.synapse.waypoint.planning.engine.PlannedDeferral;
import com.synapse.waypoint.planning.engine.PlannedStop;
import com.synapse.waypoint.planning.engine.PlannedTrip;
import com.synapse.waypoint.planning.engine.PlanningResult;
import com.synapse.waypoint.planning.entity.Deferral;
import com.synapse.waypoint.planning.entity.Plan;
import com.synapse.waypoint.planning.entity.Stop;
import com.synapse.waypoint.planning.entity.Trip;
import com.synapse.waypoint.planning.repository.DeferralRepository;
import com.synapse.waypoint.planning.repository.PlanRepository;
import com.synapse.waypoint.planning.repository.StopRepository;
import com.synapse.waypoint.planning.repository.TripRepository;

/** Writes an engine result into plans, trips, stops and deferrals, and removes a draft. */
@Component
class PlanPersister {

    private static final int FIRST_VERSION = 1;

    private final PlanRepository plans;
    private final TripRepository trips;
    private final StopRepository stops;
    private final DeferralRepository deferrals;

    PlanPersister(PlanRepository plans, TripRepository trips, StopRepository stops, DeferralRepository deferrals) {
        this.plans = plans;
        this.trips = trips;
        this.stops = stops;
        this.deferrals = deferrals;
    }

    Plan saveDraft(PlanningResult result, LocalDate runDate, String depot, String createdBy, Instant now) {
        PlanSummaryDto summary = PlanSummaryDto.of(result.summary(), result.warnings().size());
        Plan plan = plans.save(new Plan(newId("pln"), runDate, depot, FIRST_VERSION, PlanStatus.DRAFT,
                summary.toStored(), null, null, now, createdBy));
        result.trips().forEach(trip -> saveTrip(plan, trip));
        result.deferrals().forEach(deferral -> deferrals.save(toDeferral(plan, deferral)));
        return plan;
    }

    /** Deletes the plan and everything under it; flushes so a new draft can reuse the version number. */
    void deleteDraft(Plan draft) {
        stops.deleteByPlanId(draft.getId());
        trips.deleteByPlanId(draft.getId());
        deferrals.deleteByPlanId(draft.getId());
        plans.delete(draft);
        plans.flush();
    }

    private void saveTrip(Plan plan, PlannedTrip planned) {
        Trip trip = trips.save(new Trip(newId("trp"), plan.getId(), planned.vehicleId(), planned.tripNo(),
                planned.brand().label(), planned.district(), planned.windowType(),
                instantOf(plan.getRunDate(), planned.departAt()), planned.minutes(), planned.weightKg(),
                planned.volumeM3(), planned.km()));
        List<Stop> planStops = new ArrayList<>();
        for (PlannedStop stop : planned.stops()) {
            planStops.add(new Stop(newId("stp"), trip.getId(), stop.order().id(), stop.seq(), stop.loadSeq(),
                    instantOf(plan.getRunDate(), stop.arriveFrom()), instantOf(plan.getRunDate(), stop.arriveTo()),
                    stop.lateRisk()));
        }
        stops.saveAll(planStops);
    }

    private static Deferral toDeferral(Plan plan, PlannedDeferral planned) {
        return new Deferral(newId("dfr"), plan.getId(), planned.order().id(), planned.kind(), planned.rule(),
                planned.reason(), planned.priorityScore(), planned.daysWaited(), planned.newDate(),
                planned.needsDecision());
    }

    private static Instant instantOf(LocalDate runDate, LocalTime time) {
        return runDate.atTime(time).atZone(DemoClock.ZONE).toInstant();
    }

    private static String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
