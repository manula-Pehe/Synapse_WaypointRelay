package com.synapse.waypoint.driver.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.driver.entity.SyncLog;

public interface SyncLogRepository extends JpaRepository<SyncLog, String> {
}