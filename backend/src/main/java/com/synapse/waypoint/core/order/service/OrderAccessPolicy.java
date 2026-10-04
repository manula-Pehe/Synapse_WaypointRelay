package com.synapse.waypoint.core.order.service;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.core.order.entity.Order;

/**
 * Data scope for orders: a store manager sees only their own outlet's orders (anything else is
 * "not found"). Timed jobs run without a signed-in user and are not restricted.
 */
@Component
class OrderAccessPolicy {

    private final CurrentUser currentUser;

    OrderAccessPolicy(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    Order requireVisible(Order order) {
        if (!isVisible(order)) {
            throw new NotFoundException("Order", order.getId());
        }
        return order;
    }

    /** A store manager may place orders only for their own outlet. */
    void requireOwnOutletForStore(String outletId) {
        if (!isScopedTo(outletId)) {
            throw new NotFoundException("Outlet", outletId);
        }
    }

    /**
     * The outlet a list must be limited to: a store manager's own outlet whatever they asked for,
     * otherwise the outlet requested (null = all outlets).
     */
    String outletFilterFor(String requestedOutletId) {
        if (currentUser.idIfSignedIn().isEmpty() || !currentUser.is(Role.STORE_MANAGER)) {
            return requestedOutletId;
        }
        return currentUser.outletId().orElseThrow(
                () -> new DomainException(ErrorCode.FORBIDDEN, "Your account is not linked to an outlet."));
    }

    private boolean isVisible(Order order) {
        return isScopedTo(order.getOutletId());
    }

    private boolean isScopedTo(String outletId) {
        if (currentUser.idIfSignedIn().isEmpty() || !currentUser.is(Role.STORE_MANAGER)) {
            return true;
        }
        return currentUser.outletId().filter(outletId::equals).isPresent();
    }
}
