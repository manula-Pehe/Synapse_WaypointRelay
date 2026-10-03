package com.synapse.waypoint.core.reference.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.core.reference.entity.Outlet;

public interface OutletRepository extends JpaRepository<Outlet, String> {
}
