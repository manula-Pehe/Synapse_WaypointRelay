package com.synapse.waypoint.store;

import java.time.LocalDate;
import java.util.List;

/** The signed-in store manager's orders of a run, with plan data. Own outlet only. */
interface StoreDeliveryService {

    /** {@code runDate} defaults to the current run date. */
    List<DeliveryView> forRun(LocalDate runDate);
}
