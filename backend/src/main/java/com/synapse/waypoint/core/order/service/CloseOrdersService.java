package com.synapse.waypoint.core.order.service;

import java.time.LocalDate;

import com.synapse.waypoint.core.order.dto.CloseResultDto;
import com.synapse.waypoint.core.order.dto.CloseStatusDto;

/**
 * The order cut-off for one run and depot. Closing confirms the Fresh ambient orders nobody
 * confirmed; every other unconfirmed order stays PREPARED and is left out of the run.
 */
public interface CloseOrdersService {

    /** Closes the orders; fails with {@code ORDERS_CLOSED} if they are already closed. */
    CloseResultDto close(LocalDate runDate, String depot);

    CloseStatusDto status(LocalDate runDate, String depot);
}
