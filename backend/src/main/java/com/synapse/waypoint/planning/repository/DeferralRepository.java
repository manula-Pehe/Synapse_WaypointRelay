package com.synapse.waypoint.planning.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.synapse.waypoint.planning.entity.Deferral;

public interface DeferralRepository extends JpaRepository<Deferral, String> {

    List<Deferral> findByPlanId(String planId);

    List<Deferral> findByOrderId(String orderId);

    /** The order's deferrals in published plans, latest run first. */
    @Query("""
            select d from Deferral d, Plan p
            where d.planId = p.id and p.status = com.synapse.waypoint.planning.domain.PlanStatus.PUBLISHED
              and d.orderId = :orderId
            order by p.runDate desc, p.version desc""")
    List<Deferral> findPublishedByOrder(@Param("orderId") String orderId);

    /** The deferrals of the published plans of one run date. */
    @Query("""
            select d from Deferral d, Plan p
            where d.planId = p.id and p.status = com.synapse.waypoint.planning.domain.PlanStatus.PUBLISHED
              and p.runDate = :runDate""")
    List<Deferral> findPublishedByRunDate(@Param("runDate") LocalDate runDate);

    void deleteByPlanId(String planId);
}
