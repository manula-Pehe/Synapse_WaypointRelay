package com.synapse.waypoint.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.notification.entity.NotificationSeverity;
import com.synapse.waypoint.notification.service.NotificationService;

/** Notifications join the caller's transaction. Not @Transactional itself: it needs real commits and rollbacks. */
@SpringBootTest
class NotificationTransactionTests {

    private static final String USER_ID = "usr-n-tx";
    private static final String TYPE = "TX_TEST";

    @Autowired NotificationService service;
    @Autowired TransactionTemplate transaction;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void createUser() {
        OrderFixtures.insertUser(jdbc, USER_ID, "DISPATCHER", null);
    }

    @AfterEach
    void removeData() {
        jdbc.update("DELETE FROM notifications WHERE user_id = ?", USER_ID);
        jdbc.update("DELETE FROM users WHERE id = ?", USER_ID);
    }

    @Test
    void shouldLeaveNoNotificationWhenTheCallersTransactionRollsBack() {
        transaction.executeWithoutResult(status -> {
            service.notifyUser(USER_ID, NotificationSeverity.INFO, TYPE, "Title", "Body", null);
            status.setRollbackOnly();
        });

        assertThat(count()).isZero();
    }

    @Test
    void shouldKeepTheNotificationWhenTheCallersTransactionCommits() {
        transaction.executeWithoutResult(status ->
                service.notifyUser(USER_ID, NotificationSeverity.INFO, TYPE, "Title", "Body", null));

        assertThat(count()).isEqualTo(1);
    }

    private int count() {
        return jdbc.queryForObject("SELECT count(*) FROM notifications WHERE type = ?", Integer.class, TYPE);
    }
}
