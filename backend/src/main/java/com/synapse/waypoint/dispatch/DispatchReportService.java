package com.synapse.waypoint.dispatch;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.OrderFilters;
import com.synapse.waypoint.core.order.entity.OrderEvent;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.repository.OrderEventRepository;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.reference.service.DispatcherDepotScope;
import com.synapse.waypoint.dispatch.RunReportDto.DistrictLate;
import com.synapse.waypoint.dispatch.RunReportDto.ExceptionItem;
import com.synapse.waypoint.planning.dto.PlanDto;
import com.synapse.waypoint.planning.dto.PlanStopDto;
import com.synapse.waypoint.planning.entity.Deferral;
import com.synapse.waypoint.planning.repository.DeferralRepository;
import com.synapse.waypoint.planning.service.PlanQueryService;

@Service
@Transactional(readOnly = true)
public class DispatchReportService {

    private static final Set<OrderStatus> OUTCOMES = Set.of(OrderStatus.DELIVERED,
            OrderStatus.PARTIAL, OrderStatus.FAILED);

    private final DispatcherDepotScope depotScope;
    private final PlanQueryService plans;
    private final OrderService orders;
    private final OrderEventRepository events;
    private final DeferralRepository deferrals;
    private final DispatchLiveService live;

    public DispatchReportService(DispatcherDepotScope depotScope, PlanQueryService plans, OrderService orders,
            OrderEventRepository events, DeferralRepository deferrals, DispatchLiveService live) {
        this.depotScope = depotScope;
        this.plans = plans;
        this.orders = orders;
        this.events = events;
        this.deferrals = deferrals;
        this.live = live;
    }

    public RunReportDto read(LocalDate runDate, String requestedDepot) {
        String depot = depotScope.resolve(requestedDepot);
        LiveBoardDto liveBoard = live.read(runDate, depot);
        PlanDto plan = plans.publishedPlan(runDate, depot).orElse(null);
        List<OrderDto> runOrders = orders.findByRun(runDate, depot, OrderFilters.none());
        int failed = (int) runOrders.stream().filter(order -> order.status() == OrderStatus.FAILED).count();
        int partial = (int) runOrders.stream().filter(order -> order.status() == OrderStatus.PARTIAL).count();

        List<ExceptionItem> exceptions = new ArrayList<>();
        for (OrderDto order : runOrders) {
            if (order.status() == OrderStatus.FAILED || order.status() == OrderStatus.PARTIAL) {
                exceptions.add(new ExceptionItem(order.status().name(), order.ref() + " · " + order.outletName(),
                        order.id()));
            }
        }
        if (plan != null) for (Deferral deferral : deferrals.findByPlanId(plan.id())) {
            if (deferral.isNeedsDecision()) {
                exceptions.add(new ExceptionItem("DEFERRAL_DECISION", deferral.getReason(), deferral.getOrderId()));
            }
        }
        exceptions.sort(Comparator.comparing(ExceptionItem::type).thenComparing(ExceptionItem::detail));

        List<DistrictLate> districts = plan == null ? List.of() : districtLateness(plan);
        return new RunReportDto(runDate, depot, liveBoard.onTimePercent(), liveBoard.onTimeStops(),
                liveBoard.completedStops(), plan == null ? null : plan.summary().deferred(), failed, partial,
                districts, exceptions);
    }

    private List<DistrictLate> districtLateness(PlanDto plan) {
        Map<String, List<PlanStopDto>> stopsByDistrict = plan.vehicles().stream()
                .flatMap(vehicle -> vehicle.trips().stream())
                .collect(Collectors.groupingBy(trip -> trip.district(), TreeMap::new,
                        Collectors.flatMapping(trip -> trip.stops().stream(), Collectors.toList())));
        List<String> ids = stopsByDistrict.values().stream().flatMap(List::stream)
                .map(PlanStopDto::orderId).distinct().toList();
        if (ids.isEmpty()) return List.of();
        Map<String, OrderDto> byId = orders.findByIds(ids).stream()
                .collect(Collectors.toMap(OrderDto::id, Function.identity()));
        Map<String, Instant> outcomeAt = new HashMap<>();
        for (OrderEvent event : events.findByOrderIdIn(ids)) {
            if (event.getToStatus() != null && OUTCOMES.contains(event.getToStatus())) {
                outcomeAt.merge(event.getOrderId(), event.getAt(),
                        (first, second) -> first.isAfter(second) ? first : second);
            }
        }
        List<DistrictLate> result = new ArrayList<>();
        stopsByDistrict.forEach((district, stops) -> {
            int completed = 0;
            int late = 0;
            for (PlanStopDto stop : stops) {
                OrderDto order = byId.get(stop.orderId());
                Instant at = outcomeAt.get(stop.orderId());
                if (order == null || !OUTCOMES.contains(order.status()) || at == null) continue;
                completed++;
                if (at.isAfter(stop.arriveTo().toInstant())) late++;
            }
            result.add(new DistrictLate(district, late, completed));
        });
        return result;
    }
}
