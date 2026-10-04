package com.synapse.waypoint.planning.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.planning.entity.Plan;

public interface PlanRepository extends JpaRepository<Plan, String> {

    Optional<Plan> findFirstByRunDateAndDepotIgnoreCaseOrderByVersionDesc(LocalDate runDate, String depot);
}
