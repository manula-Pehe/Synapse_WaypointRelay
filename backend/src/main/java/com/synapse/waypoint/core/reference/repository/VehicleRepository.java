package com.synapse.waypoint.core.reference.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.core.reference.entity.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {
}
