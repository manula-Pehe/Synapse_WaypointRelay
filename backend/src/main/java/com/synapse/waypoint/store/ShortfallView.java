package com.synapse.waypoint.store;

/** Units the loader could not put on the truck, and the remainder order that covers them (docs/api.md §7). */
record ShortfallView(int missingUnits, String reason, String remainderOrderRef) {
}
