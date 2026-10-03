package com.synapse.waypoint.core.reference.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.core.reference.entity.VehicleAvailability;
import com.synapse.waypoint.core.reference.entity.VehicleAvailabilityId;

public interface VehicleAvailabilityRepository extends JpaRepository<VehicleAvailability, VehicleAvailabilityId> {
}
