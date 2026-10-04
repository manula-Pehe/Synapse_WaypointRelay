package com.synapse.waypoint.core.settings.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.core.settings.dto.MoveClockRequest;
import com.synapse.waypoint.core.settings.dto.SettingsResponse;
import com.synapse.waypoint.core.settings.service.SettingsService;

/** Demo clock and run date - docs/api.md §2. */
@RestController
@RequestMapping("/api/settings")
class SettingsController {

    private final SettingsService settingsService;

    SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    SettingsResponse current() {
        return settingsService.current();
    }

    @PostMapping("/clock")
    SettingsResponse moveClock(@Valid @RequestBody MoveClockRequest request) {
        return settingsService.moveClock(request.at());
    }
}
