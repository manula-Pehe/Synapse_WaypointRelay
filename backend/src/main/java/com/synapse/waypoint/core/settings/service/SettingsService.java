package com.synapse.waypoint.core.settings.service;

import java.time.OffsetDateTime;

import com.synapse.waypoint.core.settings.dto.SettingsResponse;

public interface SettingsService {

    SettingsResponse current();

    SettingsResponse moveClock(OffsetDateTime at);
}
