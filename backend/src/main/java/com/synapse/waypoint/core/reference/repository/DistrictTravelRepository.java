package com.synapse.waypoint.core.reference.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.core.reference.entity.DistrictTravel;
import com.synapse.waypoint.core.reference.entity.DistrictTravelId;

public interface DistrictTravelRepository extends JpaRepository<DistrictTravel, DistrictTravelId> {
}
