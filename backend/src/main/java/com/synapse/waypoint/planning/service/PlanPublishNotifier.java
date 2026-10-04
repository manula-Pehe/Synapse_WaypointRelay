package com.synapse.waypoint.planning.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.planning.entity.Stop;
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
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
            .withZone(ZoneId.of("Asia/Colombo"));
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

    void notifyPublished(String depot, LocalDate runDate, List<OrderDto> planned, List<Stop> stops, List<MovedOrder> moved,
            Set<String> usedVehicleIds) {
        notifyServedStores(runDate, planned, stops);
        moved.forEach(this::notifyMovedStore);
        notifications.notifyRole(Role.LOADER, NotificationScope.depot(depot), NotificationSeverity.INFO,
                "LOADING_LISTS_READY", "Loading lists ready",
                "The plan for " + DAY.format(runDate) + " is published. Loading lists are ready.", LOADER_LINK);
        new TreeSet<>(usedVehicleIds).forEach(vehicleId -> notifyDriver(runDate, vehicleId));
    }

    private void notifyServedStores(LocalDate runDate, List<OrderDto> planned, List<Stop> stops) {
        Map<String, String> outletsByOrder = planned.stream()
                .collect(Collectors.toMap(OrderDto::id, OrderDto::outletId));
        Map<String, List<Stop>> stopsByOutlet = stops.stream()
                .filter(stop -> outletsByOrder.containsKey(stop.getOrderId()))
                .collect(Collectors.groupingBy(stop -> outletsByOrder.get(stop.getOrderId())));
        new TreeSet<>(stopsByOutlet.keySet()).forEach(outletId -> {
            List<Stop> outletStops = stopsByOutlet.get(outletId);
            Stop first = outletStops.stream().min(Comparator.comparing(Stop::getArriveFrom)).orElseThrow();
            String arrival = TIME.format(first.getArriveFrom()) + "–" + TIME.format(first.getArriveTo());
            String label = outletStops.size() == 1 ? "Expected arrival: " : "First expected arrival: ";
            notifications.notifyRole(Role.STORE_MANAGER, NotificationScope.outlet(outletId),
                    NotificationSeverity.INFO, "DELIVERY_WINDOW", "Delivery scheduled for " + DAY.format(runDate),
                    "Your delivery is scheduled for " + DAY.format(runDate) + ". " + label + arrival + ".",
                    STORE_DELIVERIES_LINK);
        });
    }

    private void notifyMovedStore(MovedOrder moved) {
        OrderDto order = moved.order();
        notifications.notifyRole(Role.STORE_MANAGER, NotificationScope.outlet(order.outletId()),
                NotificationSeverity.CRITICAL, "DELIVERY_DEFERRED",
                "Order " + order.ref() + " deferred to " + DAY.format(order.runDate()),
                "Order " + order.ref() + " has been deferred to " + DAY.format(order.runDate())
                        + ". Reason: " + moved.reason(),
                STORE_ORDER_LINK + order.id());
    }

    private void notifyDriver(LocalDate runDate, String vehicleId) {
        notifications.notifyRole(Role.DRIVER, NotificationScope.vehicle(vehicleId), NotificationSeverity.INFO,
                "TRIPS_READY", "Your trips are ready",
                "Your trips for " + DAY.format(runDate) + " are published.", DRIVER_LINK);
    }
}
