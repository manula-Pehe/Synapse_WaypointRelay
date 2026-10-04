package com.synapse.waypoint.core.job;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.core.order.dto.CloseResultDto;
import com.synapse.waypoint.core.order.service.CloseOrdersService;
import com.synapse.waypoint.core.order.service.CutOffSchedule;

/**
 * 4 PM on the day before the run: closes the orders as the system. Orders the dispatcher already
 * closed by hand count as success.
 */
@Component
class CloseOrdersJob extends DemoClockJob {

    static final String NAME = "close-orders";

    private final CutOffSchedule cutOff;
    private final CloseOrdersService closeOrders;

    CloseOrdersJob(CutOffSchedule cutOff, CloseOrdersService closeOrders) {
        this.cutOff = cutOff;
        this.closeOrders = closeOrders;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Instant triggerAt(LocalDate runDate) {
        return cutOff.cutOffFor(runDate).toInstant();
    }

    @Override
    public JobOutcome run(JobRun run) {
        if (closeOrders.status(run.runDate(), run.depot()).closed()) {
            return JobOutcome.skipped("orders already closed");
        }
        CloseResultDto result = closeOrders.close(run.runDate(), run.depot());
        return JobOutcome.completed("confirmed " + result.confirmed() + ", auto-confirmed " + result.autoConfirmed()
                + ", left out " + result.notConfirmed());
    }
}
