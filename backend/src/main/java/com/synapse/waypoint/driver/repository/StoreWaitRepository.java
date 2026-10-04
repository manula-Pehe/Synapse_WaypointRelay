package com.synapse.waypoint.driver.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.driver.entity.StoreWait;

/** Reads of the waits drivers recorded */
public interface StoreWaitRepository extends JpaRepository<StoreWait, String> {

    List<StoreWait> findByStopIdOrderByStartedAtDesc(String stopId);
}