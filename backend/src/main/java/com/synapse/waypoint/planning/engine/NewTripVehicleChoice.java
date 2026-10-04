package com.synapse.waypoint.planning.engine;

/** Which vehicle a new trip tries first, among those that pass the hard rules. */
public enum NewTripVehicleChoice {
    SMALLEST_THAT_FITS,
    LARGEST_THAT_FITS
}
