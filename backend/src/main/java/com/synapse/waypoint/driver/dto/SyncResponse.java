package com.synapse.waypoint.driver.dto;

import java.util.List;

import com.synapse.waypoint.driver.entity.SyncResult;

/**
 * The body POST /api/sync returns one result per item, in the order the
 * items were sent. The phone marks APPLIED and DUPLICATE as done, and surfaces CONFLICT on its sync
 * summary (R6) because a dispatcher has a decision to make.
 */
public record SyncResponse(List<Result> results) {

    public SyncResponse {
        results = List.copyOf(results);
    }

    public record Result(String clientId, SyncResult result, String entityId) {
    }
}