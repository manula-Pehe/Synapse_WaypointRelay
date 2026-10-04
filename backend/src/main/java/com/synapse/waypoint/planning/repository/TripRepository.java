package com.synapse.waypoint.planning.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.planning.entity.Trip;

public interface TripRepository extends JpaRepository<Trip, String> {

    List<Trip> findByPlanIdOrderByVehicleIdAscTripNoAsc(String planId);
}
