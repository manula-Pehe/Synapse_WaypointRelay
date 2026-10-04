package com.synapse.waypoint.store;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** The predicted arrival window of a planned order (docs/api.md §7); {@code changedReason} is set by plan changes. */
record ArrivalView(OffsetDateTime from, OffsetDateTime to, BigDecimal lateRisk, String vehicleId, int tripNo,
        String changedReason) {
}
