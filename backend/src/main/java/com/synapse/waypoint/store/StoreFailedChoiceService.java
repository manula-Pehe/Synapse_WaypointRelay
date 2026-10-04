package com.synapse.waypoint.store;

import com.synapse.waypoint.driver.entity.FailedDeliveryDecision;

/** The store manager's answer to a delivery that could not be completed (S3f). */
interface StoreFailedChoiceService {

    /**
     * Records what the store wants done with a failed delivery of its own outlet.
     *
     * @throws com.synapse.waypoint.common.error.NotFoundException if the delivery is not the caller's
     * @throws com.synapse.waypoint.common.error.DomainException INVALID_STATUS if it did not fail,
     *         DUPLICATE if it was already decided
     */
    void choose(String deliveryId, FailedDeliveryDecision choice);
}
