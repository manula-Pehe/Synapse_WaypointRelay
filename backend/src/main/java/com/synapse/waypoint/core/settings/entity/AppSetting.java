package com.synapse.waypoint.core.settings.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One key/value system setting (table {@code app_settings}, V1). */
@Entity
@Table(name = "app_settings")
public class AppSetting {

    @Id
    @Column(name = "key", length = 60)
    private String key;

    @Column(name = "value", nullable = false, length = 200)
    private String value;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppSetting() {
        // for JPA
    }

    public AppSetting(String key, String value, Instant updatedAt) {
        this.key = key;
        this.value = value;
        this.updatedAt = updatedAt;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(String newValue, Instant at) {
        this.value = newValue;
        this.updatedAt = at;
    }
}
