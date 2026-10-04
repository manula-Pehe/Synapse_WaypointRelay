package com.synapse.waypoint.core.job;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

/** Invented users and notification look-ups shared by the job tests. */
final class JobTestData {

    private JobTestData() {
    }

    static void insertUser(JdbcTemplate jdbc, String id, String role, String outletId, String depot) {
        jdbc.update("""
                INSERT INTO users (id, name, role, email, secret_hash, outlet_id, depot, active)
                VALUES (?, ?, ?, ?, 'x', ?, ?, true)""", id, "Test " + role, role, id + "@example.lk", outletId, depot);
    }

    static List<String> recipientsOf(JdbcTemplate jdbc, String type) {
        return jdbc.queryForList("SELECT user_id FROM notifications WHERE type = ? ORDER BY user_id", String.class,
                type);
    }
}
