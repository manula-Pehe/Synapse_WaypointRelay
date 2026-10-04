package com.synapse.waypoint.core.reference.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.synapse.waypoint.core.reference.entity.VehicleAvailability;
import com.synapse.waypoint.core.reference.entity.VehicleAvailabilityId;

public interface VehicleAvailabilityRepository extends JpaRepository<VehicleAvailability, VehicleAvailabilityId> {

    @Query("select a from VehicleAvailability a where a.id.runDate = :runDate")
    List<VehicleAvailability> findByRunDate(@Param("runDate") LocalDate runDate);
}
