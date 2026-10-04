package com.synapse.waypoint.core.reference.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.core.reference.entity.AvailabilityStatus;
import com.synapse.waypoint.core.reference.entity.Vehicle;
import com.synapse.waypoint.core.reference.entity.VehicleAvailability;
import com.synapse.waypoint.core.reference.repository.VehicleAvailabilityRepository;

/**
 * The one place that decides whether a vehicle can be planned on a run date: a vehicle with no
 * availability row for that date is AVAILABLE.
 */
@Component
class FleetAvailabilityPolicy {

    /** A vehicle's availability on one run date. */
    record Availability(AvailabilityStatus status, String reason) {

        static final Availability DEFAULT = new Availability(AvailabilityStatus.AVAILABLE, null);

        boolean isAvailable() {
            return status == AvailabilityStatus.AVAILABLE;
        }
    }

    private final VehicleAvailabilityRepository availability;

    FleetAvailabilityPolicy(VehicleAvailabilityRepository availability) {
        this.availability = availability;
    }

    /** The availability of each given vehicle on the run date, keyed by vehicle id. */
    Map<String, Availability> forVehicles(LocalDate runDate, Collection<Vehicle> vehicles) {
        Map<String, VehicleAvailability> recorded = availability.findByRunDate(runDate).stream()
                .collect(Collectors.toMap(row -> row.getId().vehicleId(), Function.identity()));
        Map<String, Availability> result = new HashMap<>();
        for (Vehicle vehicle : vehicles) {
            VehicleAvailability row = recorded.get(vehicle.getId());
            result.put(vehicle.getId(),
                    row == null ? Availability.DEFAULT : new Availability(row.getStatus(), row.getReason()));
        }
        return Map.copyOf(result);
    }
}
