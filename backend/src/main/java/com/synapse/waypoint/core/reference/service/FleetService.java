package com.synapse.waypoint.core.reference.service;

import java.time.LocalDate;

import com.synapse.waypoint.core.reference.dto.ConfirmFleetRequest;
import com.synapse.waypoint.core.reference.dto.FleetConfirmationDto;
import com.synapse.waypoint.core.reference.dto.FleetDto;
import com.synapse.waypoint.core.reference.dto.UpdateAvailabilityRequest;
import com.synapse.waypoint.core.reference.dto.VehicleDto;

/** The dispatcher's fleet for a run: who can drive, and the confirmation that the fleet is final (D2). */
public interface FleetService {

    /** Every vehicle of the depot with its availability, the summary counts and the confirmation. */
    FleetDto view(LocalDate runDate, String depot);

    /**
     * Sets a vehicle's availability for a run date (D2v). A reason is required unless the vehicle is
     * AVAILABLE. A vehicle of another depot than the dispatcher's is reported as not found.
     */
    VehicleDto setAvailability(String vehicleId, UpdateAvailabilityRequest request);

    /** Confirms the fleet of a run; confirming again updates the time. */
    FleetConfirmationDto confirm(ConfirmFleetRequest request);
}
