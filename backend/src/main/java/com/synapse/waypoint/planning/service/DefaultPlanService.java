package com.synapse.waypoint.planning.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.reference.service.DispatcherDepotScope;
import com.synapse.waypoint.planning.domain.PlanStatus;
import com.synapse.waypoint.planning.dto.DeferralDto;
import com.synapse.waypoint.planning.dto.PlanDto;
import com.synapse.waypoint.planning.dto.PlanReadinessDto;
import com.synapse.waypoint.planning.engine.PlanningEngine;
import com.synapse.waypoint.planning.engine.PlanningResult;
import com.synapse.waypoint.planning.entity.Deferral;
import com.synapse.waypoint.planning.entity.Plan;
import com.synapse.waypoint.planning.entity.Stop;
import com.synapse.waypoint.planning.entity.Trip;
import com.synapse.waypoint.planning.input.PlanningInputLoader;
import com.synapse.waypoint.planning.repository.DeferralRepository;
import com.synapse.waypoint.planning.repository.PlanRepository;
import com.synapse.waypoint.planning.repository.StopRepository;
import com.synapse.waypoint.planning.repository.TripRepository;
import com.synapse.waypoint.planning.service.PlanPublishNotifier.MovedOrder;

@Service
@Transactional
class DefaultPlanService implements PlanService {

    private static final String PLAN = "Plan";

    private final PlanRepository plans;
    private final TripRepository trips;
    private final StopRepository stops;
    private final DeferralRepository deferrals;
    private final PlanningInputLoader inputLoader;
    private final PlanningEngine engine;
    private final PlanPersister persister;
    private final PlanViewLoader views;
    private final PlanReadinessChecker readinessChecker;
    private final PlanPublishNotifier notifier;
    private final OrderService orders;
    private final DispatcherDepotScope depotScope;
    private final DemoClock clock;
    private final CurrentUser currentUser;

    DefaultPlanService(PlanRepository plans, TripRepository trips, StopRepository stops,
            DeferralRepository deferrals, PlanningInputLoader inputLoader, PlanningEngine engine,
            PlanPersister persister, PlanViewLoader views, PlanReadinessChecker readiness,
            PlanPublishNotifier notifier, OrderService orders, DispatcherDepotScope depotScope, DemoClock clock,
            CurrentUser currentUser) {
        this.plans = plans;
        this.trips = trips;
        this.stops = stops;
        this.deferrals = deferrals;
        this.inputLoader = inputLoader;
        this.engine = engine;
        this.persister = persister;
        this.views = views;
        this.readinessChecker = readiness;
        this.notifier = notifier;
        this.orders = orders;
        this.depotScope = depotScope;
        this.clock = clock;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional(readOnly = true)
    public PlanReadinessDto readiness(LocalDate runDate, String depot) {
        return readinessChecker.check(runDate, depotScope.resolve(depot));
    }

    @Override
    public PlanDto create(LocalDate runDate, String requestedDepot) {
        String depot = depotScope.resolve(requestedDepot);
        requireClosedOrders(runDate, depot);
        requireNoPublishedPlan(runDate, depot);
        plans.findByRunDateAndDepotIgnoreCaseAndStatus(runDate, depot, PlanStatus.DRAFT).forEach(persister::deleteDraft);
        PlanningResult result = engine.plan(inputLoader.load(runDate, depot));
        Plan plan = persister.saveDraft(result, runDate, depot, currentUser.id(), clock.now());
        return views.plan(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanDto latest(LocalDate runDate, String requestedDepot) {
        String depot = depotScope.resolve(requestedDepot);
        return views.plan(plans.findFirstByRunDateAndDepotIgnoreCaseOrderByVersionDesc(runDate, depot)
                .orElseThrow(() -> new NotFoundException(PLAN, runDate + " " + depot)));
    }

    @Override
    @Transactional(readOnly = true)
    public PlanDto get(String planId) {
        return views.plan(visiblePlan(planId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeferralDto> deferrals(String planId) {
        return views.deferrals(visiblePlan(planId).getId());
    }

    @Override
    public PlanDto publish(String planId) {
        Plan plan = plans.findByIdForUpdate(planId).orElseThrow(() -> new NotFoundException(PLAN, planId));
        depotScope.requireInScope(plan.getDepot());
        requireDraft(plan);
        List<Trip> planTrips = trips.findByPlanIdOrderByVehicleIdAscTripNoAsc(plan.getId());
        List<OrderDto> planned = markPlanned(plan, planTrips);
        List<MovedOrder> moved = markMoved(plan);
        plan.publish(currentUser.id(), clock.now());
        plans.save(plan);
        Set<String> usedVehicles = planTrips.stream().map(Trip::getVehicleId).collect(Collectors.toSet());
        notifier.notifyPublished(plan.getDepot(), plan.getRunDate(), planned, moved, usedVehicles);
        return views.plan(plan);
    }

    private List<OrderDto> markPlanned(Plan plan, List<Trip> planTrips) {
        if (planTrips.isEmpty()) {
            return List.of();
        }
        List<Stop> planStops = stops.findByTripIdInOrderByTripIdAscSeqAsc(planTrips.stream().map(Trip::getId).toList());
        return planStops.stream().map(stop -> orders.markPlanned(stop.getOrderId(), plan.getId())).toList();
    }

    private List<MovedOrder> markMoved(Plan plan) {
        return deferrals.findByPlanId(plan.getId()).stream().map(this::markMoved).toList();
    }

    private MovedOrder markMoved(Deferral deferral) {
        OrderDto order = orders.markMoved(deferral.getOrderId(), deferral.getNewDate(), deferral.getReason());
        return new MovedOrder(order, deferral.getReason());
    }

    private Plan visiblePlan(String planId) {
        Plan plan = plans.findById(planId).orElseThrow(() -> new NotFoundException(PLAN, planId));
        depotScope.requireInScope(plan.getDepot());
        return plan;
    }

    private void requireClosedOrders(LocalDate runDate, String depot) {
        if (!orders.isClosed(runDate, depot)) {
            throw new DomainException(ErrorCode.ORDERS_NOT_CLOSED,
                    "Close the orders before creating the plan.", Map.of("runDate", runDate.toString(), "depot", depot));
        }
    }

    private void requireNoPublishedPlan(LocalDate runDate, String depot) {
        if (plans.findFirstByRunDateAndDepotIgnoreCaseAndStatus(runDate, depot, PlanStatus.PUBLISHED).isPresent()) {
            throw new DomainException(ErrorCode.PLAN_LOCKED, "This run is already published. Use a plan change instead.",
                    Map.of("runDate", runDate.toString(), "depot", depot));
        }
    }

    private static void requireDraft(Plan plan) {
        if (plan.getStatus() != PlanStatus.DRAFT) {
            throw new DomainException(ErrorCode.PLAN_LOCKED, "This plan is already published.",
                    Map.of("planId", plan.getId(), "status", plan.getStatus().name()));
        }
    }
}
