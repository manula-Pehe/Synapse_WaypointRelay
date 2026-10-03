package com.synapse.waypoint.core.order.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.time.ApiTimestamp;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.dto.CloseResultDto;
import com.synapse.waypoint.core.order.dto.CloseStatusDto;
import com.synapse.waypoint.core.order.dto.OrderFilters;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderRun;
import com.synapse.waypoint.core.order.entity.OrderRunId;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.repository.OrderRunRepository;
import com.synapse.waypoint.core.order.repository.OrderSpecifications;
import com.synapse.waypoint.core.reference.repository.OutletRepository;

@Service
@Transactional
class DefaultCloseOrdersService implements CloseOrdersService {

    private final OrderRunRepository runs;
    private final OrderRepository orders;
    private final OrderService orderService;
    private final OutletRepository outlets;
    private final AutoConfirmPolicy autoConfirmPolicy;
    private final CutOffSchedule cutOff;
    private final DemoClock clock;
    private final CurrentUser currentUser;

    DefaultCloseOrdersService(OrderRunRepository runs, OrderRepository orders, OrderService orderService,
            OutletRepository outlets, AutoConfirmPolicy autoConfirmPolicy, CutOffSchedule cutOff, DemoClock clock,
            CurrentUser currentUser) {
        this.runs = runs;
        this.orders = orders;
        this.orderService = orderService;
        this.outlets = outlets;
        this.autoConfirmPolicy = autoConfirmPolicy;
        this.cutOff = cutOff;
        this.clock = clock;
        this.currentUser = currentUser;
    }

    @Override
    public CloseResultDto close(LocalDate runDate, String depot) {
        String depotName = canonicalDepot(depot);
        OrderRun run = lockRun(runDate, depotName);
        if (run.isClosed()) {
            throw new DomainException(ErrorCode.ORDERS_CLOSED, "Orders for this run are already closed.",
                    Map.of("runDate", runDate.toString(), "depot", depotName));
        }
        List<Order> runOrders = orders.findAll(OrderSpecifications.forRun(runDate, depotName, OrderFilters.none()));
        int alreadyConfirmed = count(runOrders, OrderStatus.CONFIRMED);
        List<Order> unconfirmed = runOrders.stream().filter(o -> o.getStatus() == OrderStatus.PREPARED).toList();
        List<Order> toAutoConfirm = unconfirmed.stream().filter(autoConfirmPolicy::appliesTo).toList();
        toAutoConfirm.forEach(order -> orderService.autoConfirm(order.getId()));

        run.closeOrders(currentUser.idIfSignedIn().orElse(null), clock.now());
        runs.saveAndFlush(run);
        return new CloseResultDto(ApiTimestamp.of(run.getOrdersClosedAt()), alreadyConfirmed, toAutoConfirm.size(),
                unconfirmed.size() - toAutoConfirm.size());
    }

    @Override
    @Transactional(readOnly = true)
    public CloseStatusDto status(LocalDate runDate, String depot) {
        return runs.findByRunDateAndDepot(runDate, depot.strip())
                .filter(OrderRun::isClosed)
                .map(run -> new CloseStatusDto(true, ApiTimestamp.of(run.getOrdersClosedAt()),
                        run.getOrdersClosedBy(), cutOff.cutOffFor(runDate)))
                .orElseGet(() -> new CloseStatusDto(false, null, null, cutOff.cutOffFor(runDate)));
    }

    private OrderRun lockRun(LocalDate runDate, String depotName) {
        runs.createIfAbsent(runDate, depotName);
        return runs.findForUpdate(new OrderRunId(runDate, depotName)).orElseThrow();
    }

    /** The depot as the outlets spell it, so "peliyagoda" and "Peliyagoda" are one run. */
    private String canonicalDepot(String depot) {
        return outlets.findDepotName(depot.strip()).orElseThrow(() -> new DomainException(ErrorCode.VALIDATION,
                "Unknown depot.", Map.of("depot", "no outlet belongs to this depot")));
    }

    private static int count(List<Order> runOrders, OrderStatus status) {
        return (int) runOrders.stream().filter(o -> o.getStatus() == status).count();
    }
}
