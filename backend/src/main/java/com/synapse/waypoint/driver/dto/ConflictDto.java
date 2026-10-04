package com.synapse.waypoint.driver.dto;

import java.time.Instant;
import java.util.Map;

import com.synapse.waypoint.driver.entity.ConflictResolution;
import com.synapse.waypoint.driver.entity.ConflictStatus;

/**
 * A clash between a driver's offline delivery and a change made on the board.
 *
 * <p>details carries both sides of the clash so the card can explain itself without the
 * dispatcher opening anything else.
 */
public record ConflictDto(
        String id,
        String stopId,
        String orderId,
        String deliveryId,
        Map<String, Object> details,
        ConflictStatus status,
        ConflictResolution resolution,
        Instant createdAt) {

    public ConflictDto {
        details = Map.copyOf(details);
    }
}