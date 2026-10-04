package com.synapse.waypoint.driver.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * The body of  POST /api/sync.
 *
 * <p>items is in the order the driver acted and is applied in that order: an arrival before
 * its delivery, a delivery before its undo.
 */
public record SyncRequest(@NotEmpty(message = "nothing to sync") @Valid List<SyncItem> items) {

    public SyncRequest {
        items = items == null ? List.of() : List.copyOf(items);
    }
}