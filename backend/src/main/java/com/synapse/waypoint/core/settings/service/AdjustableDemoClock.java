package com.synapse.waypoint.core.settings.service;

import java.time.Instant;

import com.synapse.waypoint.common.time.DemoClock;

/** A {@link DemoClock} that can be moved. Only the settings module moves it. */
public interface AdjustableDemoClock extends DemoClock {

    void moveTo(Instant at);
}
