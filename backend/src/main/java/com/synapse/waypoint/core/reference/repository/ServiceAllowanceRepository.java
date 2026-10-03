package com.synapse.waypoint.core.reference.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.core.reference.entity.ServiceAllowance;
import com.synapse.waypoint.core.reference.entity.ServiceAllowanceId;

public interface ServiceAllowanceRepository extends JpaRepository<ServiceAllowance, ServiceAllowanceId> {
}
