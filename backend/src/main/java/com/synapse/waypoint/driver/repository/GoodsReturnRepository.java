package com.synapse.waypoint.driver.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.driver.entity.GoodsReturn;

/** Reads of the goods handed back at the depot (R8r). */
public interface GoodsReturnRepository extends JpaRepository<GoodsReturn, String> {

    List<GoodsReturn> findByTripIdOrderByRecordedAtDesc(String tripId);
}
