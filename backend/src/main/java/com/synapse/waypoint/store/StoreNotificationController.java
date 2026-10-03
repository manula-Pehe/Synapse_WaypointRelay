package com.synapse.waypoint.store;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.common.security.CurrentUser;

/** Device-independent store alert choices and read state for backend-derived notices. */
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

    @GetMapping("/reads")
    List<String> reads() {
        return jdbc.queryForList("SELECT notification_id FROM store_notification_reads WHERE user_id = ?",
                String.class, user.id());
    }

    @PostMapping("/reads")
    void markRead(@Valid @RequestBody ReadRequest request) {
        for (String id : request.ids()) {
            if (id == null || id.isBlank() || id.length() > 160) continue;
            jdbc.update("INSERT INTO store_notification_reads(user_id, notification_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                    user.id(), id);
        }
    }

    record Settings(@NotNull Boolean deliveries, @NotNull Boolean orders, @NotNull Boolean issues) {}
    record ReadRequest(@NotNull @Size(max = 100) List<String> ids) {}
}
