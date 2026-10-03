package com.synapse.waypoint.core.order.dto;

import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;

/** Optional narrowing of an order list; a null field means "any". */
public record OrderFilters(OrderStatus status, String outletId, String brand, TemperatureRequirement temp) {

    public static OrderFilters none() {
        return new OrderFilters(null, null, null, null);
    }

    public OrderFilters forOutlet(String outlet) {
        return new OrderFilters(status, outlet, brand, temp);
    }
}
