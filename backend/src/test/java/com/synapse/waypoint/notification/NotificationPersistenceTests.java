package com.synapse.waypoint.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.notification.entity.Notification;
import com.synapse.waypoint.notification.entity.NotificationSeverity;
import com.synapse.waypoint.notification.repository.NotificationRepository;

/** The entity must match V2 exactly (ddl-auto=validate); this saves and reloads one. Values are invented. */
@SpringBootTest
@Transactional
class NotificationPersistenceTests {

    private static final Instant CREATED_AT = Instant.parse("2026-09-30T08:30:00Z");

    @Autowired NotificationRepository notifications;
    @Autowired JdbcTemplate jdbc;

    @Test
    void shouldSaveAndReloadANotification() {
        OrderFixtures.insertUser(jdbc, "usr-n-persist", "DISPATCHER", null);
        notifications.saveAndFlush(new Notification("ntf-persist", "usr-n-persist", NotificationSeverity.CRITICAL,
                "DELIVERY_FAILED", "Failed delivery", "Store closed.", "/dispatch/live", CREATED_AT));

        Notification loaded = notifications.findById("ntf-persist").orElseThrow();

        assertThat(loaded.getSeverity()).isEqualTo(NotificationSeverity.CRITICAL);
        assertThat(loaded.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(loaded.getReadAt()).isEmpty();
    }

    @Test
    void shouldRecordTheFirstReadTimeOnly() {
        OrderFixtures.insertUser(jdbc, "usr-n-read", "DISPATCHER", null);
        Notification saved = notifications.saveAndFlush(new Notification("ntf-read", "usr-n-read",
                NotificationSeverity.INFO, "NOTE", "Title", "Body", null, CREATED_AT));

        saved.markRead(CREATED_AT.plusSeconds(60));
        saved.markRead(CREATED_AT.plusSeconds(120));

        assertThat(saved.getReadAt()).contains(CREATED_AT.plusSeconds(60));
    }
}
