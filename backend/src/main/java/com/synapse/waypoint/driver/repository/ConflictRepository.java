package com.synapse.waypoint.driver.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.driver.entity.Conflict;
import com.synapse.waypoint.driver.entity.ConflictStatus;

/** The conflicts the dispatcher decides on. */
public interface ConflictRepository extends JpaRepository<Conflict, String> {

    List<Conflict> findByStatusOrderByCreatedAtDesc(ConflictStatus status);

    List<Conflict> findByOrderIdOrderByCreatedAtDesc(String orderId);

    boolean existsByStopIdAndStatus(String stopId, ConflictStatus status);
}