package com.synapse.waypoint.planning.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.synapse.waypoint.planning.domain.PlanStatus;
import com.synapse.waypoint.planning.entity.Plan;

public interface PlanRepository extends JpaRepository<Plan, String> {

    Optional<Plan> findFirstByRunDateAndDepotIgnoreCaseOrderByVersionDesc(LocalDate runDate, String depot);

    List<Plan> findByRunDateAndDepotIgnoreCaseAndStatus(LocalDate runDate, String depot, PlanStatus status);

    Optional<Plan> findFirstByRunDateAndDepotIgnoreCaseAndStatus(LocalDate runDate, String depot, PlanStatus status);

    /** Locks the row so two publishes of the same plan run one after the other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Plan p where p.id = :id")
    Optional<Plan> findByIdForUpdate(@Param("id") String id);
}
