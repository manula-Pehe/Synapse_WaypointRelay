package com.synapse.waypoint.core.reference.service;

/** How an outlet is named on screens until outlets have a name column: "OUT001 · District". */
public final class OutletNames {

    private static final String FORMAT = "%s · %s";

    private OutletNames() {
    }

    public static String of(String outletId, String district) {
        return FORMAT.formatted(outletId, district);
    }
}
