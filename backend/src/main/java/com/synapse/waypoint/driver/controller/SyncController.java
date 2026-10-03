package com.synapse.waypoint.driver.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.driver.dto.SyncRequest;
import com.synapse.waypoint.driver.dto.SyncResponse;
import com.synapse.waypoint.driver.service.SyncService;

/**  POST /api/sync. Shared by the driver app and, if time, the loader app. */
@RestController
@RequestMapping("/api/sync")
class SyncController {

    private final SyncService syncService;
    private final CurrentUser currentUser;

    SyncController(SyncService syncService, CurrentUser currentUser) {
        this.syncService = syncService;
        this.currentUser = currentUser;
    }

    /**
     * Applies a driver's queued actions in order. Safe to call with the same body twice: each item is
     * keyed by the phone's own clientId, so a retry is answered DUPLICATE rather than applied again.
     */
    @PostMapping
    SyncResponse sync(@Valid @RequestBody SyncRequest request) {
        return syncService.sync(currentUser.id(), request);
    }
}