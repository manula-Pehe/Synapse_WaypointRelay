package com.synapse.waypoint.store;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.common.security.CurrentUser;

/** Device-independent store alert choices. */
@RestController
@RequestMapping("/api/store/notifications")
class StoreNotificationController {
    private final JdbcTemplate jdbc;
    private final CurrentUser user;

    StoreNotificationController(JdbcTemplate jdbc, CurrentUser user) {
        this.jdbc = jdbc;
        this.user = user;
    }

    @GetMapping("/settings")
    Settings settings() {
        return jdbc.query("SELECT deliveries, orders, issues FROM store_notification_settings WHERE user_id = ?",
                (rs, row) -> new Settings(rs.getBoolean(1), rs.getBoolean(2), rs.getBoolean(3)), user.id())
                .stream().findFirst().orElse(new Settings(true, true, true));
    }

    @PutMapping("/settings")
    Settings saveSettings(@Valid @RequestBody Settings settings) {
        jdbc.update("""
                INSERT INTO store_notification_settings(user_id, deliveries, orders, issues)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET deliveries = EXCLUDED.deliveries,
                    orders = EXCLUDED.orders, issues = EXCLUDED.issues""",
                user.id(), settings.deliveries(), settings.orders(), settings.issues());
        return settings;
    }

    record Settings(@NotNull Boolean deliveries, @NotNull Boolean orders, @NotNull Boolean issues) {}
}
