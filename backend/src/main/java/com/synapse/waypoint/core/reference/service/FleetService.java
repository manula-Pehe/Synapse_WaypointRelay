package com.synapse.waypoint.core.reference.service;

import java.time.LocalDate;

import com.synapse.waypoint.core.reference.dto.FleetDto;

/** The dispatcher's fleet for a run: who can drive, and the confirmation that the fleet is final (D2). */
public interface FleetService {

    /** Every vehicle of the depot with its availability, the summary counts and the confirmation. */
    FleetDto view(LocalDate runDate, String depot);
}
