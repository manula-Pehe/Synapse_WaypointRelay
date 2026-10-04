package com.synapse.waypoint.driver.service;

import com.synapse.waypoint.driver.dto.SyncRequest;
import com.synapse.waypoint.driver.dto.SyncResponse;

/**
 * Applies actions queued on a driver's phone .
 *
 * <p>Called only by  POST /api/sync. The loader app posts to the same endpoint.
 */
public interface SyncService {

    /**
     * Applies rquest in order for userId. An action whose clientId has been
     * seen before is answered DUPLICATE and not applied again, so a phone that reconnects mid-request
     * can retry safely.
     */
    SyncResponse sync(String userId, SyncRequest request);
}