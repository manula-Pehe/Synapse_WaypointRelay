package com.synapse.waypoint.driver.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.driver.entity.ProblemStatus;
import com.synapse.waypoint.driver.entity.VehicleProblem;

/** The problems drivers reported from the cab, read by the live board and the re-plan. */
public interface VehicleProblemRepository extends JpaRepository<VehicleProblem, String> {

    List<VehicleProblem> findByStatusOrderByReportedAtDesc(ProblemStatus status);

    List<VehicleProblem> findByVehicleIdOrderByReportedAtDesc(String vehicleId);

    List<VehicleProblem> findAllByOrderByReportedAtDesc();
}