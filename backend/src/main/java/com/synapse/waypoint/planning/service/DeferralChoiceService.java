package com.synapse.waypoint.planning.service;

import com.synapse.waypoint.planning.domain.StoreChoice;
import com.synapse.waypoint.planning.dto.DeferralDto;

/** A store manager's answer to the deferral of one of their orders (docs/api.md §6, Store-facing). */
public interface DeferralChoiceService {

    /**
     * Records the signed-in store manager's choice for the deferral and applies it to the order.
     *
     * @param units required for REDUCE (1 up to the order's units minus one), optional for SPLIT, ignored otherwise
     * @return the deferral with {@code storeChoice} set
     */
    DeferralDto choose(String deferralId, StoreChoice choice, Integer units);
}
