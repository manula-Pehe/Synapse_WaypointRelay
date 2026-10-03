package com.synapse.waypoint.common.seed;

/** Fixed facts of the seeded demo day that more than one module's seed step relies on. */
public final class DemoDay {

    /** Booklet peak-day scenario used as the demo run. */
    public static final String SCENARIO = "S1";

    /** Dilani's outlet; its orders start Prepared so the walkthrough can confirm them. */
    public static final String STORE_OUTLET_ID = "OUT001";

    /** Nuwan's vehicle. */
    public static final String DRIVER_VEHICLE_ID = "VEH036";

    /** Depot of the loader account. */
    public static final String LOADER_DEPOT = "Peliyagoda";

    private DemoDay() {
    }
}
