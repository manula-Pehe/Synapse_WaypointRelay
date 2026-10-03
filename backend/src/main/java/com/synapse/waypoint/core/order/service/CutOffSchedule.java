package com.synapse.waypoint.core.order.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.time.DemoClock;

/** When orders for a run close: 4 PM Sri Lanka time on the day before the run date. */
@Component
public class CutOffSchedule {

    static final LocalTime CUT_OFF_TIME = LocalTime.of(16, 0);

    public OffsetDateTime cutOffFor(LocalDate runDate) {
        return runDate.minusDays(1).atTime(CUT_OFF_TIME).atZone(DemoClock.ZONE).toOffsetDateTime();
    }
}
