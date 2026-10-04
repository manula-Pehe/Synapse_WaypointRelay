package com.synapse.waypoint.planning.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.dto.OrderFilters;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.reference.dto.FleetDto;
import com.synapse.waypoint.core.reference.service.FleetService;
import com.synapse.waypoint.planning.dto.PlanReadinessDto;

/** Answers Dp0: are orders closed, is the fleet confirmed, and is there anything to plan with. */
@Component
class PlanReadinessChecker {

    private final OrderService orders;
    private final FleetService fleet;

    PlanReadinessChecker(OrderService orders, FleetService fleet) {
        this.orders = orders;
        this.fleet = fleet;
    }

    @Transactional(readOnly = true)
    PlanReadinessDto check(LocalDate runDate, String depot) {
        boolean ordersClosed = orders.isClosed(runDate, depot);
        int confirmedOrders = orders.findByRun(runDate, depot, new OrderFilters(OrderStatus.CONFIRMED, null, null,
                null)).size();
        FleetDto fleetView = fleet.view(runDate, depot);
        boolean fleetConfirmed = fleetView.confirmedAt() != null;
        int available = fleetView.counts().available();
        int reefers = fleetView.counts().reeferAvailable();
        return new PlanReadinessDto(ordersClosed, fleetConfirmed, confirmedOrders, available, reefers,
                warnings(ordersClosed, fleetConfirmed, confirmedOrders, available, reefers));
    }

    private static List<String> warnings(boolean ordersClosed, boolean fleetConfirmed, int confirmedOrders,
            int available, int reefers) {
        List<String> warnings = new ArrayList<>();
        if (!ordersClosed) {
            warnings.add("Orders are not closed yet.");
        }
        if (!fleetConfirmed) {
            warnings.add("The fleet is not confirmed yet.");
        }
        if (confirmedOrders == 0) {
            warnings.add("There are no confirmed orders to plan.");
        }
        if (available == 0) {
            warnings.add("No vehicle is available.");
        } else if (reefers == 0) {
            warnings.add("No fridge vehicle is available, so chilled orders cannot be planned.");
        }
        return warnings;
    }
}
