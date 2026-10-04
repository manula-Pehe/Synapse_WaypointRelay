package com.synapse.waypoint.core.order.service;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderRun;
import com.synapse.waypoint.core.order.repository.OrderRunRepository;
import com.synapse.waypoint.core.reference.entity.Outlet;
import com.synapse.waypoint.core.reference.repository.OutletRepository;

/**
 * The single place that decides whether orders may still change once the cut-off has passed.
 * A store manager can no longer change an order of a closed run; the dispatcher and timed jobs
 * still can. New orders cannot be added to a closed run by anyone.
 */
@Component
class OrderChangeGuard {

    private final OrderRunRepository runs;
    private final OutletRepository outlets;
    private final CurrentUser currentUser;

    OrderChangeGuard(OrderRunRepository runs, OutletRepository outlets, CurrentUser currentUser) {
        this.runs = runs;
        this.outlets = outlets;
        this.currentUser = currentUser;
    }

    void requireOpenForChanges(Order order) {
        if (isStoreManager()) {
            requireRunOpen(order.getOutletId(), order.getRunDate());
        }
    }

    void requireOpenForNewOrder(String outletId, LocalDate runDate) {
        requireRunOpen(outletId, runDate);
    }

    private void requireRunOpen(String outletId, LocalDate runDate) {
        String depot = outlets.findById(outletId).map(Outlet::getDepot)
                .orElseThrow(() -> new NotFoundException("Outlet", outletId));
        if (runs.findByRunDateAndDepot(runDate, depot).filter(OrderRun::isClosed).isPresent()) {
            throw new DomainException(ErrorCode.ORDERS_CLOSED, "Orders closed - changes go to the next run",
                    Map.of("runDate", runDate.toString(), "depot", depot));
        }
    }

    private boolean isStoreManager() {
        return currentUser.idIfSignedIn().isPresent() && currentUser.is(Role.STORE_MANAGER);
    }
}
