package com.synapse.waypoint.core.order.service;

import java.time.LocalDate;
import java.util.List;

import com.synapse.waypoint.core.order.dto.UnconfirmedOutletDto;

/** The stores the dispatcher still has to chase before the cut-off (D1u). */
public interface UnconfirmedOrderService {

    /** PREPARED orders of the run (and depot, if given), grouped by outlet and ordered by outlet id. */
    List<UnconfirmedOutletDto> unconfirmed(LocalDate runDate, String depot);
}
