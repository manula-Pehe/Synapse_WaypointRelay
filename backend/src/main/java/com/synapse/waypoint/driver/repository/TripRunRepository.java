package com.synapse.waypoint.driver.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.driver.entity.TripRun;

public interface TripRunRepository extends JpaRepository<TripRun, String> {
}