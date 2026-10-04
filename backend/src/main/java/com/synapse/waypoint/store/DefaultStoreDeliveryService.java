package com.synapse.waypoint.store;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.planning.dto.DeferralDto;
import com.synapse.waypoint.planning.dto.StopPlacementDto;
import com.synapse.waypoint.planning.dto.StoreDeferralDto;
import com.synapse.waypoint.planning.service.PlanQueryService;

/**
 * An order moved away from a run now carries its new date, so a run lists the outlet's orders dated
 * that day plus the orders that run's published plan deferred.
 */
@Service
@Transactional(readOnly = true)
class DefaultStoreDeliveryService implements StoreDeliveryService {

    private final StoreOrderAccess access;
    private final OrderService orders;
    private final PlanQueryService plan;
    private final StoreReceipts receipts;
    private final StoreDeliveryFacts facts;
    private final DemoClock clock;

    DefaultStoreDeliveryService(StoreOrderAccess access, OrderService orders, PlanQueryService plan,
            StoreReceipts receipts, StoreDeliveryFacts facts, DemoClock clock) {
        this.access = access;
        this.orders = orders;
        this.plan = plan;
        this.receipts = receipts;
        this.facts = facts;
        this.clock = clock;
    }

    @Override
    public List<DeliveryView> forRun(LocalDate runDate) {
        LocalDate date = runDate == null ? clock.runDate() : runDate;
        String outletId = access.outletId();
        Map<String, DeferralDto> deferredFromRun = plan.deferralsForOutlet(date, outletId).stream()
                .collect(Collectors.toMap(DeferralDto::orderId, Function.identity(), (first, second) -> first));
        List<OrderDto> listed = ordersOf(date, deferredFromRun);
        Map<String, ReceiptView> receiptsByOrder = receipts.forOrders(listed.stream().map(OrderDto::id).toList());
        return listed.stream()
                .map(order -> view(order, deferredFromRun.get(order.id()), receiptsByOrder.get(order.id())))
                .toList();
    }

    private List<OrderDto> ordersOf(LocalDate date, Map<String, DeferralDto> deferredFromRun) {
        List<OrderDto> dated = access.find(date, date);
        List<String> datedIds = dated.stream().map(OrderDto::id).toList();
        List<String> movedAway = deferredFromRun.keySet().stream().filter(id -> !datedIds.contains(id)).toList();
        return Stream.concat(dated.stream(), orders.findByIds(movedAway).stream())
                .sorted(Comparator.comparing(OrderDto::ref))
                .toList();
    }

    private DeliveryView view(OrderDto order, DeferralDto deferredFromRun, ReceiptView receipt) {
        ArrivalView arrival = arrival(order).orElse(null);
        return new DeliveryView(order.id(), order.ref(), order.status(), arrival,
                deferral(order, deferredFromRun).map(StoreDeferralDto::from).orElse(null),
                facts.delivery(order.id()).orElse(null), facts.shortfall(order.id()).orElse(null),
                facts.driverStatus(order.status(), arrival).orElse(null), receipt);
    }

    private Optional<ArrivalView> arrival(OrderDto order) {
        return plan.stopForOrder(order.id()).map(DefaultStoreDeliveryService::toArrival);
    }

    /** The deferral of the run being listed, else the one that moved a MOVED order to its current date. */
    private Optional<DeferralDto> deferral(OrderDto order, DeferralDto deferredFromRun) {
        if (deferredFromRun != null) {
            return Optional.of(deferredFromRun);
        }
        return order.status() == OrderStatus.MOVED ? plan.deferralForOrder(order.id()) : Optional.empty();
    }

    private static ArrivalView toArrival(StopPlacementDto placement) {
        return new ArrivalView(placement.stop().arriveFrom(), placement.stop().arriveTo(),
                placement.stop().lateRisk(), placement.vehicleId(), placement.tripNo(), null);
    }
}
