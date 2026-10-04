package com.synapse.waypoint.planning.choice;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.planning.domain.StoreChoice;

/** The store accepts the new date: the order stays as it is. */
@Component
class KeepEffect implements ChoiceEffect {

    @Override
    public StoreChoice choice() {
        return StoreChoice.KEEP;
    }

    @Override
    public void apply(ChoiceContext context) {
        // Nothing to change: the order is already moved to its new date.
    }
}
