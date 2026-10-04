package com.synapse.waypoint.core.job;

import java.time.LocalTime;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.synapse.waypoint.core.job.hook.ReceiptAutoClose;

/** 11:59 PM on the run day: receipts the store never confirmed are closed automatically. */
@Component
class ReceiptAutoCloseJob extends RunDayHookJob<ReceiptAutoClose> {

    static final String NAME = "receipt-auto-close";

    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59);

    ReceiptAutoCloseJob(ObjectProvider<ReceiptAutoClose> autoClose) {
        super(autoClose, END_OF_DAY);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected void invoke(ReceiptAutoClose autoClose, JobRun run) {
        autoClose.closeUnconfirmedReceipts(run.runDate(), run.depot());
    }
}
