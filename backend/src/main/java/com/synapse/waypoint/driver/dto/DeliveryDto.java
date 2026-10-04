package com.synapse.waypoint.driver.dto;

import java.time.Instant;

import com.synapse.waypoint.core.order.entity.DeliveryOutcome;
import com.synapse.waypoint.driver.entity.DeliveryReason;
import com.synapse.waypoint.driver.entity.FailedDeliveryDecision;

/**
 * What a driver did at one stop, as the rest of the app reads it 
 */
public record DeliveryDto(
        String id,
        String stopId,
        String orderId,
        String vehicleId,
        DeliveryOutcome outcome,
        int units,
        DeliveryReason reason,
        String receivedBy,
        boolean hasPhoto,
        boolean hasSignature,
        Instant completedAt,
        boolean undone,
        FailedDeliveryDecision decision,
        FailedDeliveryDecision storeChoice) {

    /** True once a dispatcher has settled it (D6f) or a store has answered. */
    public boolean isDecided() {
        return decision != null;
    }
}