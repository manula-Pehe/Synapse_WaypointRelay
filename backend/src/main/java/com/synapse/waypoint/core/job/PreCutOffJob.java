package com.synapse.waypoint.core.job;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.synapse.waypoint.core.order.dto.UnconfirmedOutletDto;
import com.synapse.waypoint.core.order.service.CloseOrdersService;
import com.synapse.waypoint.core.order.service.CutOffSchedule;
import com.synapse.waypoint.core.order.service.UnconfirmedOrderService;

/**
 * A notification job that chases unconfirmed orders before the cut-off. It does nothing once the
 * orders are closed, and sends nothing when there is nobody to chase; either way it counts as done.
 */
abstract class PreCutOffJob extends DemoClockJob {

    private final CutOffSchedule cutOff;
    private final CloseOrdersService closeOrders;
    private final UnconfirmedOrderService unconfirmedOrders;

    protected PreCutOffJob(CutOffSchedule cutOff, CloseOrdersService closeOrders,
            UnconfirmedOrderService unconfirmedOrders) {
        this.cutOff = cutOff;
        this.closeOrders = closeOrders;
        this.unconfirmedOrders = unconfirmedOrders;
    }

    /** How long before the cut-off the job is due. */
    protected abstract Duration leadTime();

    /** The stores this job reports on, out of everyone with unconfirmed orders. */
    protected abstract List<UnconfirmedOutletDto> storesToChase(List<UnconfirmedOutletDto> unconfirmed);

    protected abstract void notifyAbout(JobRun run, List<UnconfirmedOutletDto> stores);

    @Override
    public final Instant triggerAt(LocalDate runDate) {
        OffsetDateTime cutOffTime = cutOff.cutOffFor(runDate);
        return cutOffTime.minus(leadTime()).toInstant();
    }

    @Override
    public final JobOutcome run(JobRun run) {
        if (closeOrders.status(run.runDate(), run.depot()).closed()) {
            return JobOutcome.skipped("orders already closed");
        }
        List<UnconfirmedOutletDto> stores = storesToChase(unconfirmedOrders.unconfirmed(run.runDate(), run.depot()));
        if (stores.isEmpty()) {
            return JobOutcome.skipped("nothing to report");
        }
        notifyAbout(run, stores);
        return JobOutcome.completed("reported " + (stores.size() == 1 ? "1 store" : stores.size() + " stores"));
    }
}
