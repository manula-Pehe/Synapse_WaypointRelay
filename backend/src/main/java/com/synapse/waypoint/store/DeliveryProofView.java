package com.synapse.waypoint.store;

import java.time.Instant;

import com.synapse.waypoint.core.order.entity.DeliveryOutcome;

/** What the driver recorded at the door (docs/api.md §7); photo and signature link to {@code /api/files/{id}}. */
record DeliveryProofView(DeliveryOutcome outcome, int units, String photoUrl, String signatureUrl,
        String receivedBy, Instant at) {
}
