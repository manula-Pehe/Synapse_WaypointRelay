package com.synapse.waypoint.core.job.hook;

import java.time.LocalDate;

/**
 * Implemented by the store module. At the end of the run day, receipts the store never confirmed
 * are closed automatically. Until an implementation exists the job skips itself.
 */
public interface ReceiptAutoClose {

    void closeUnconfirmedReceipts(LocalDate runDate, String depot);
}
