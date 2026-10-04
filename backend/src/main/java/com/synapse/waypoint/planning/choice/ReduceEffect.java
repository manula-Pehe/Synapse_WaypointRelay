package com.synapse.waypoint.planning.choice;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.planning.domain.StoreChoice;

/** The store takes fewer units, hoping a smaller order fits. */
@Component
class ReduceEffect implements ChoiceEffect {

    private final OrderService orders;

    ReduceEffect(OrderService orders) {
        this.orders = orders;
    }

    @Override
    public StoreChoice choice() {
        return StoreChoice.REDUCE;
    }

    @Override
    public boolean takesUnits() {
        return true;
    }

    @Override
    public void validate(ChoiceContext context) {
        ChoiceUnits.requireSmallerThanOrdered(context.order(), context.units());
    }

    @Override
    public void apply(ChoiceContext context) {
        orders.resizeMoved(context.order().id(), context.units());
    }
}
