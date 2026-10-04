package com.synapse.waypoint.planning.service;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.reference.service.ReferenceService;
import com.synapse.waypoint.notification.entity.NotificationSeverity;
import com.synapse.waypoint.notification.recipient.NotificationScope;
import com.synapse.waypoint.notification.service.NotificationService;
import com.synapse.waypoint.planning.domain.StoreChoice;

/** Tells the dispatchers of the outlet's depot what a store decided about a deferred order. */
@Component
class DeferralChoiceNotifier {

    private static final String TYPE = "STORE_DEFERRAL_CHOICE";
    private static final String DISPATCH_LINK = "/dispatch";

    private final NotificationService notifications;
    private final ReferenceService reference;

    DeferralChoiceNotifier(NotificationService notifications, ReferenceService reference) {
        this.notifications = notifications;
        this.reference = reference;
    }

    void notifyDispatchers(OrderDto order, StoreChoice choice, Integer units) {
        String depot = reference.outlet(order.outletId()).depot();
        notifications.notifyRole(Role.DISPATCHER, NotificationScope.depot(depot), NotificationSeverity.INFO, TYPE,
                "Store chose " + choice + " for " + order.ref(), body(order, choice, units), DISPATCH_LINK);
    }

    private static String body(OrderDto order, StoreChoice choice, Integer units) {
        String detail = units == null ? "" : " (" + units + " of " + order.units() + " units)";
        return order.outletName() + " answered the deferral of order " + order.ref() + " with " + choice + detail + ".";
    }
}
