package com.synapse.waypoint.store;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.core.order.entity.OrderSource;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.event.OrderStatusChanged;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.notification.entity.NotificationSeverity;
import com.synapse.waypoint.notification.recipient.NotificationScope;
import com.synapse.waypoint.notification.service.NotificationService;

/** Sends store notices in the same transaction as the order change. */
@Component
class StoreOrderNotifier {
    private final OrderRepository orders;
    private final NotificationService notifications;

    StoreOrderNotifier(OrderRepository orders, NotificationService notifications) {
        this.orders = orders;
        this.notifications = notifications;
    }

    @EventListener
    void onOrderChanged(OrderStatusChanged change) {
        if (change.from() == change.to() ||
                (change.to() != OrderStatus.CONFIRMED && change.to() != OrderStatus.DELIVERED
                        && change.to() != OrderStatus.PARTIAL && change.to() != OrderStatus.FAILED)) return;
        var order = orders.findById(change.orderId()).orElseThrow();
        String link = "/store/orders/" + order.getId();
        NotificationScope scope = NotificationScope.outlet(order.getOutletId());
        if (change.to() == OrderStatus.DELIVERED) {
            notifications.notifyRole(Role.STORE_MANAGER, scope, NotificationSeverity.INFO,
                    "DELIVERY_COMPLETED", "Order " + order.getRef() + " delivered",
                    "Order " + order.getRef() + " has been delivered. Confirm receipt when the driver's proof is available.",
                    "/store/deliveries/" + order.getId() + "/receipt");
        } else if (change.to() == OrderStatus.PARTIAL || change.to() == OrderStatus.FAILED) {
            notifications.notifyRole(Role.STORE_MANAGER, scope, NotificationSeverity.CRITICAL,
                    "DELIVERY_PROBLEM", "Delivery problem for " + order.getRef(),
                    change.to() == OrderStatus.PARTIAL
                            ? "Only part of order " + order.getRef() + " was delivered. Review what arrived and report any problem."
                            : "The driver reported a problem completing order " + order.getRef()
                                    + ". Await an updated decision from dispatch.",
                    "/store/deliveries/" + order.getId() + "/problem");
        } else if (change.from() == null && order.getSource() == OrderSource.PHONE_IN) {
            notifications.notifyRole(Role.STORE_MANAGER, scope, NotificationSeverity.WARNING,
                    "PHONE_ORDER_CHECK", "Check order " + order.getRef(),
                    "Dispatch added " + order.getUnits() + " cases for " + order.getRunDate()
                            + ". Check the order and confirm its details or report a correction.", link);
        } else if (change.from() != null) {
            notifications.notifyRole(Role.STORE_MANAGER, scope, NotificationSeverity.INFO,
                    "ORDER_CONFIRMED", "Order " + order.getRef() + " confirmed",
                    "Your order of " + order.getUnits() + " cases for " + order.getRunDate()
                            + (change.actorUserId() == null ? " was confirmed automatically." : " is confirmed."), link);
        }
    }
}
