package com.synapse.waypoint.dispatch;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.time.ApiTimestamp;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.OrderFilters;
import com.synapse.waypoint.core.order.entity.OrderEvent;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.repository.OrderEventRepository;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.reference.service.DispatcherDepotScope;
import com.synapse.waypoint.dispatch.LiveBoardDto.Attention;
import com.synapse.waypoint.dispatch.LiveBoardDto.TripProgress;
import com.synapse.waypoint.planning.dto.PlanDto;
import com.synapse.waypoint.planning.dto.PlanStopDto;
import com.synapse.waypoint.planning.dto.PlanTripDto;
import com.synapse.waypoint.planning.entity.Deferral;
import com.synapse.waypoint.planning.repository.DeferralRepository;
import com.synapse.waypoint.planning.service.PlanQueryService;

@Service
@Transactional(readOnly = true)
public class DispatchLiveService {

    private static final Set<OrderStatus> COMPLETED = Set.of(OrderStatus.DELIVERED,
            OrderStatus.PARTIAL, OrderStatus.FAILED);

    private final DispatcherDepotScope depotScope;
    private final PlanQueryService plans;
    private final OrderService orders;
    private final OrderEventRepository events;
    private final DeferralRepository deferrals;
    private final DemoClock clock;

    public DispatchLiveService(DispatcherDepotScope depotScope, PlanQueryService plans, OrderService orders,
            OrderEventRepository events, DeferralRepository deferrals, DemoClock clock) {
        this.depotScope = depotScope;
        this.plans = plans;
        this.orders = orders;
        this.events = events;
        this.deferrals = deferrals;
        this.clock = clock;
    }

    public LiveBoardDto read(LocalDate runDate, String requestedDepot) {
        String depot = depotScope.resolve(requestedDepot);
        PlanDto plan = plans.publishedPlan(runDate, depot).orElse(null);
        List<OrderDto> runOrders = orders.findByRun(runDate, depot, OrderFilters.none());
        Map<String, OrderDto> byId = runOrders.stream().collect(Collectors.toMap(OrderDto::id, Function.identity()));
        List<PlanTripDto> plannedTrips = plan == null ? List.of() : plan.vehicles().stream()
                .flatMap(vehicle -> vehicle.trips().stream()).toList();
        List<String> stopOrderIds = plannedTrips.stream().flatMap(trip -> trip.stops().stream())
                .map(PlanStopDto::orderId).distinct().toList();
        orders.findByIds(stopOrderIds).forEach(order -> byId.put(order.id(), order));
        Map<String, Instant> outcomes = outcomeTimes(stopOrderIds);

        int completed = 0;
        int onTime = 0;
        List<Attention> attention = new ArrayList<>();
        List<TripProgress> trips = new ArrayList<>();
        for (PlanTripDto trip : plannedTrips) {
            int done = 0;
            boolean started = false;
            OffsetDateTime lastUpdate = null;
            for (PlanStopDto stop : trip.stops()) {
                OrderDto order = byId.get(stop.orderId());
                if (order == null) continue;
                if (COMPLETED.contains(order.status())) {
                    done++;
                    Instant at = outcomes.get(order.id());
                    if (at != null) {
                        completed++;
                        if (!at.isAfter(stop.arriveTo().toInstant())) onTime++;
                    }
                }
                if (order.status() == OrderStatus.ON_THE_WAY || COMPLETED.contains(order.status())) started = true;
                if (lastUpdate == null || order.updatedAt().isAfter(lastUpdate)) lastUpdate = order.updatedAt();
                if (stop.lateRisk().signum() > 0 && !COMPLETED.contains(order.status())) {
                    attention.add(new Attention("risk-" + stop.id(), "LATE_RISK", "WARNING",
                            "Planned late risk · " + stop.outletId(),
                            "Arrival window " + stop.arriveFrom() + "–" + stop.arriveTo()
                                    + " · plan risk " + stop.lateRisk(), order.id(), "Open order"));
                }
            }
            String status = done == trip.stops().size() && done > 0 ? "COMPLETED"
                    : started ? "ON_THE_WAY" : "PLANNED";
            String vehicleId = plan.vehicles().stream().filter(vehicle -> vehicle.trips().contains(trip))
                    .map(vehicle -> vehicle.vehicleId()).findFirst().orElse("");
            trips.add(new TripProgress(trip.id(), vehicleId, trip.tripNo(), trip.district(), done,
                    trip.stops().size(), status, lastUpdate, null));
        }
        for (OrderDto order : runOrders) {
            if (order.status() == OrderStatus.FAILED) {
                attention.add(new Attention("failed-" + order.id(), "FAILED_DELIVERY", "CRITICAL",
                        "Failed delivery · " + order.outletName(), order.ref() + " · " + order.units()
                                + " units", order.id(), "Open order"));
            }
        }
        if (plan != null) for (Deferral deferral : deferrals.findByPlanId(plan.id())) {
            if (deferral.isNeedsDecision()) {
                attention.add(new Attention("deferral-" + deferral.getId(), "DEFERRAL_DECISION", "WARNING",
                        "Deferral needs a decision", deferral.getReason(), deferral.getOrderId(), "Open order"));
            }
        }
        attention.sort(Comparator.comparingInt((Attention item) -> "CRITICAL".equals(item.severity()) ? 0 : 1)
                .thenComparing(Attention::title));
        Integer onTimePercent = completed == 0 ? null : (int) Math.round(100.0 * onTime / completed);
        Integer fridgeUse = plan == null || plan.summary().fridgeVehiclesAvailable() == 0 ? null
                : (int) Math.round(100.0 * plan.summary().fridgeVehiclesUsed()
                        / plan.summary().fridgeVehiclesAvailable());
        return new LiveBoardDto(runDate, depot, ApiTimestamp.of(clock.now()), onTimePercent, completed,
                onTime, plan == null ? null : plan.summary().deferred(), null, fridgeUse, attention, trips);
    }

    private Map<String, Instant> outcomeTimes(List<String> orderIds) {
        if (orderIds.isEmpty()) return Map.of();
        Map<String, Instant> found = new HashMap<>();
        for (OrderEvent event : events.findByOrderIdIn(orderIds)) {
            if (event.getToStatus() != null && COMPLETED.contains(event.getToStatus())) {
                found.merge(event.getOrderId(), event.getAt(), (first, second) -> first.isAfter(second) ? first : second);
            }
        }
        return found;
    }
}
