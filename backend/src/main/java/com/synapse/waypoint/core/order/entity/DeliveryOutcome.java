package com.synapse.waypoint.core.order.entity;

/** How a delivery ended (docs/api.md, Enumerations); each outcome is also an order status. */
public enum DeliveryOutcome {
    DELIVERED(OrderStatus.DELIVERED),
    PARTIAL(OrderStatus.PARTIAL),
    FAILED(OrderStatus.FAILED);

    private final OrderStatus status;

    DeliveryOutcome(OrderStatus status) {
        this.status = status;
    }

    public OrderStatus status() {
        return status;
    }
}
