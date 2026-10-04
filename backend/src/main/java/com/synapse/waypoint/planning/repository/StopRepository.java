package com.synapse.waypoint.planning.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.planning.entity.Stop;

public interface StopRepository extends JpaRepository<Stop, String> {

    List<Stop> findByTripIdInOrderByTripIdAscSeqAsc(Collection<String> tripIds);

    List<Stop> findByOrderId(String orderId);
}
