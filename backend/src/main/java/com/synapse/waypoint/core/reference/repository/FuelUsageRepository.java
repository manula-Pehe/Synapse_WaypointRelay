package com.synapse.waypoint.core.reference.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.core.reference.entity.FuelUsage;
import com.synapse.waypoint.core.reference.entity.FuelUsageId;

public interface FuelUsageRepository extends JpaRepository<FuelUsage, FuelUsageId> {
}
