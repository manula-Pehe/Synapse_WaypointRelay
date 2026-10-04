package com.synapse.waypoint.planning.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.synapse.waypoint.planning.entity.Trip;

public interface TripRepository extends JpaRepository<Trip, String> {

    List<Trip> findByPlanIdOrderByVehicleIdAscTripNoAsc(String planId);

    void deleteByPlanId(String planId);

    @Query("""
            select t from Trip t, Plan p
            where t.planId = p.id and p.status = com.synapse.waypoint.planning.domain.PlanStatus.PUBLISHED
              and p.runDate = :runDate and t.vehicleId = :vehicleId
            order by t.tripNo""")
    List<Trip> findPublishedByRunDateAndVehicle(@Param("runDate") LocalDate runDate,
            @Param("vehicleId") String vehicleId);

    @Query("""
            select t from Trip t, Stop s, Plan p
            where s.tripId = t.id and t.planId = p.id
              and p.status = com.synapse.waypoint.planning.domain.PlanStatus.PUBLISHED and s.orderId = :orderId""")
    List<Trip> findPublishedByOrder(@Param("orderId") String orderId);
}
