package com.synapse.waypoint.store;

import java.time.Instant;

record ReceiptView(int receivedUnits, Instant at) {
}
