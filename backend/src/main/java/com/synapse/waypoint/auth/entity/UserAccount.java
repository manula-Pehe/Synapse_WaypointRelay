package com.synapse.waypoint.auth.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.synapse.waypoint.common.security.Role;

/**
 * A person who signs in (table {@code users}, V1). Outlet, depot and vehicle are kept as plain IDs
 * so the auth module does not depend on core entities.
 */
@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @Column(length = 40)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(length = 120)
    private String email;

    @Column(name = "staff_id", length = 20)
    private String staffId;

    @Column(name = "secret_hash", nullable = false, length = 100)
    private String secretHash;

    @Column(name = "outlet_id", length = 10)
    private String outletId;

    @Column(length = 20)
    private String depot;

    @Column(name = "vehicle_id", length = 10)
    private String vehicleId;

    @Column(nullable = false, length = 2)
    private String language;

    @Column(nullable = false, length = 10)
    private String theme;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserAccount() {
        // for JPA
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public String getStaffId() {
        return staffId;
    }

    public String getSecretHash() {
        return secretHash;
    }

    public String getOutletId() {
        return outletId;
    }

    public String getDepot() {
        return depot;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getLanguage() {
        return language;
    }

    public String getTheme() {
        return theme;
    }

    public boolean isActive() {
        return active;
    }

    /** Changes the user's display preferences; a {@code null} value keeps the current one. */
    public void updatePreferences(String newLanguage, String newTheme) {
        if (newLanguage != null) {
            this.language = newLanguage;
        }
        if (newTheme != null) {
            this.theme = newTheme;
        }
    }
}
