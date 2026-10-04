package com.synapse.waypoint.planning.choice;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.planning.domain.StoreChoice;

/** The store no longer wants the order. */
@Component
class CancelEffect implements ChoiceEffect {

    static final String REASON = "Store cancelled after deferral";

    private final OrderService orders;

    CancelEffect(OrderService orders) {
        this.orders = orders;
    }

    @Override
    public StoreChoice choice() {
        return StoreChoice.CANCEL;
    }

    @Override
    public void apply(ChoiceContext context) {
        orders.cancelMoved(context.order().id(), REASON);
    }
}
