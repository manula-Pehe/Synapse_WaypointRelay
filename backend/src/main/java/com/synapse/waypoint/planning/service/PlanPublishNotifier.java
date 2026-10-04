package com.synapse.waypoint.planning.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.notification.entity.NotificationSeverity;
import com.synapse.waypoint.notification.recipient.NotificationScope;
import com.synapse.waypoint.notification.service.NotificationService;

/**
 * Tells stores, loaders and drivers that a plan is published. Called inside the publishing
 * transaction, after the order changes, so a rolled-back publish sends nothing.
 */
@Component
class PlanPublishNotifier {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
    private static final String STORE_DELIVERIES_LINK = "/store/deliveries";
    private static final String STORE_ORDER_LINK = "/store/orders/";
    private static final String LOADER_LINK = "/loader";
    private static final String DRIVER_LINK = "/driver";

    /** An order the plan moved to a later date, and the reason the store is told. */
    record MovedOrder(OrderDto order, String reason) {
    }

    private final NotificationService notifications;

    PlanPublishNotifier(NotificationService notifications) {
        this.notifications = notifications;
    }

    void notifyPublished(String depot, LocalDate runDate, List<OrderDto> planned, List<MovedOrder> moved,
            Set<String> usedVehicleIds) {
        notifyServedStores(runDate, planned);
        moved.forEach(this::notifyMovedStore);
        notifications.notifyRole(Role.LOADER, NotificationScope.depot(depot), NotificationSeverity.INFO,
                "LOADING_LISTS_READY", "Loading lists ready",
                "The plan for " + DAY.format(runDate) + " is published. Loading lists are ready.", LOADER_LINK);
        new TreeSet<>(usedVehicleIds).forEach(vehicleId -> notifyDriver(runDate, vehicleId));
    }

    private void notifyServedStores(LocalDate runDate, List<OrderDto> planned) {
        Set<String> outletIds = planned.stream().map(OrderDto::outletId)
                .collect(Collectors.toCollection(TreeSet::new));
        outletIds.forEach(outletId -> notifications.notifyRole(Role.STORE_MANAGER, NotificationScope.outlet(outletId),
                NotificationSeverity.INFO, "DELIVERY_WINDOW", "Delivery window for " + DAY.format(runDate),
                "Your delivery is planned for " + DAY.format(runDate) + ". Open it to see the arrival window.",
                STORE_DELIVERIES_LINK));
    }

    private void notifyMovedStore(MovedOrder moved) {
        OrderDto order = moved.order();
        notifications.notifyRole(Role.STORE_MANAGER, NotificationScope.outlet(order.outletId()),
                NotificationSeverity.WARNING, "ORDER_MOVED",
                "Order " + order.ref() + " moved to " + DAY.format(order.runDate()), moved.reason(),
                STORE_ORDER_LINK + order.id());
    }

    private void notifyDriver(LocalDate runDate, String vehicleId) {
        notifications.notifyRole(Role.DRIVER, NotificationScope.vehicle(vehicleId), NotificationSeverity.INFO,
                "TRIPS_READY", "Your trips are ready",
                "Your trips for " + DAY.format(runDate) + " are published.", DRIVER_LINK);
    }
}
