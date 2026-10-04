package com.synapse.waypoint.core.reference.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.synapse.waypoint.core.reference.entity.DistrictTravel;
import com.synapse.waypoint.core.reference.entity.DistrictTravelId;

public interface DistrictTravelRepository extends JpaRepository<DistrictTravel, DistrictTravelId> {

    @Query("""
            select t from DistrictTravel t
            where lower(t.id.district) = lower(:district) and lower(t.id.depot) = lower(:depot)""")
    Optional<DistrictTravel> findByDistrictAndDepot(@Param("district") String district, @Param("depot") String depot);
}
