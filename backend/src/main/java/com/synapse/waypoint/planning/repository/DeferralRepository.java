package com.synapse.waypoint.planning.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.planning.entity.Deferral;

public interface DeferralRepository extends JpaRepository<Deferral, String> {

    List<Deferral> findByPlanId(String planId);

    List<Deferral> findByOrderId(String orderId);
}
