package com.synapse.waypoint.core.reference.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.synapse.waypoint.core.reference.entity.Outlet;

public interface OutletRepository extends JpaRepository<Outlet, String> {

    /** The depot's name as the outlets spell it, matched ignoring case. */
    @Query("select min(o.depot) from Outlet o where lower(o.depot) = lower(:depot)")
    Optional<String> findDepotName(@Param("depot") String depot);

    List<Outlet> findAllByOrderByIdAsc();

    List<Outlet> findByDepotIgnoreCaseOrderByIdAsc(String depot);
}
