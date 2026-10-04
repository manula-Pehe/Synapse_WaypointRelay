package com.synapse.waypoint.planning.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.synapse.waypoint.planning.entity.Stop;

public interface StopRepository extends JpaRepository<Stop, String> {

    List<Stop> findByTripIdInOrderByTripIdAscSeqAsc(Collection<String> tripIds);

    List<Stop> findByOrderId(String orderId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Stop s where s.tripId in (select t.id from Trip t where t.planId = :planId)")
    void deleteByPlanId(@Param("planId") String planId);
}
