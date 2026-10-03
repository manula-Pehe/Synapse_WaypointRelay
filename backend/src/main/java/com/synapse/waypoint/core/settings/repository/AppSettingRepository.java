package com.synapse.waypoint.core.settings.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.core.settings.entity.AppSetting;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
