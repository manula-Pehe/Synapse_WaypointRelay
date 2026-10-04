package com.synapse.waypoint.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.support.ApiSignIn;

/** The notification endpoints over HTTP, as each role. Users and notifications are invented. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationApiTests {

    private static final String PASSWORD = "Test-Password-1";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void createData() {
        insertUser("usr-na-disp", "DISPATCHER", "na.disp@example.lk");
        insertUser("usr-na-other", "DISPATCHER", "na.other@example.lk");
        insertNotification("ntf-na-1", "usr-na-disp", "INFO", "Oldest", "2026-09-30T09:00:00Z", null);
        insertNotification("ntf-na-2", "usr-na-disp", "CRITICAL", "Middle", "2026-09-30T10:00:00Z",
                "2026-09-30T10:30:00Z");
        insertNotification("ntf-na-3", "usr-na-disp", "WARNING", "Newest", "2026-09-30T11:00:00Z", null);
        insertNotification("ntf-na-x", "usr-na-other", "INFO", "Not mine", "2026-09-30T12:00:00Z", null);
    }

    @Test
    void shouldListOwnNotificationsNewestFirstWithUnreadCount() throws Exception {
        mvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.unreadCount").value(2))
                .andExpect(jsonPath("$.items[0].id").value("ntf-na-3"))
                .andExpect(jsonPath("$.items[1].id").value("ntf-na-2"))
                .andExpect(jsonPath("$.items[2].id").value("ntf-na-1"))
                .andExpect(jsonPath("$.items[0].readAt").doesNotExist())
                .andExpect(jsonPath("$.items[1].readAt").exists())
                .andExpect(jsonPath("$.items[0].createdAt").value("2026-09-30T16:30:00+05:30"));
    }

    @Test
    void shouldListOnlyUnreadWhenAsked() throws Exception {
        mvc.perform(get("/api/notifications?unread=true").header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.unreadCount").value(2))
                .andExpect(jsonPath("$.items[0].id").value("ntf-na-3"))
                .andExpect(jsonPath("$.items[1].id").value("ntf-na-1"));
    }

    @Test
    void shouldKeepCriticalNotificationsInTheList() throws Exception {
        mvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(jsonPath("$.items[?(@.severity == 'CRITICAL')].id").value("ntf-na-2"));
    }

    @Test
    void shouldMarkOneNotificationRead() throws Exception {
        String bearer = dispatcher();

        mvc.perform(post("/api/notifications/ntf-na-3/read").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ntf-na-3"))
                .andExpect(jsonPath("$.readAt").exists());
        mvc.perform(get("/api/notifications?unread=true").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    void shouldKeepTheFirstReadTimeWhenMarkedReadAgain() throws Exception {
        mvc.perform(post("/api/notifications/ntf-na-2/read").header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readAt").value("2026-09-30T16:00:00+05:30"));
    }

    @Test
    void shouldMarkAllReadAndCountOnlyTheNewlyRead() throws Exception {
        String bearer = dispatcher();

        mvc.perform(post("/api/notifications/read-all").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));
        mvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.unreadCount").value(0));
        mvc.perform(post("/api/notifications/read-all").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.updated").value(0));
    }

    @Test
    void shouldLeaveOtherUsersNotificationsUnreadOnReadAll() throws Exception {
        mvc.perform(post("/api/notifications/read-all").header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isOk());

        mvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, other()))
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    void shouldAnswerNotFoundForAnotherUsersNotification() throws Exception {
        mvc.perform(post("/api/notifications/ntf-na-x/read").header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, other()))
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    void shouldAnswerNotFoundForAnUnknownNotification() throws Exception {
        mvc.perform(post("/api/notifications/ntf-na-nope/read").header(HttpHeaders.AUTHORIZATION, dispatcher()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRefuseRequestsWithoutSignIn() throws Exception {
        mvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/notifications/read-all")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldBeOpenToEveryRole() throws Exception {
        insertUser("usr-na-store", "STORE_MANAGER", "na.store@example.lk");
        insertUser("usr-na-loader", "LOADER", "na.loader@example.lk");
        insertUser("usr-na-driver", "DRIVER", "na.driver@example.lk");
        for (String email : new String[] {"na.store@example.lk", "na.loader@example.lk", "na.driver@example.lk"}) {
            mvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION,
                            ApiSignIn.bearer(mvc, email, PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0));
        }
    }

    private String dispatcher() throws Exception {
        return ApiSignIn.bearer(mvc, "na.disp@example.lk", PASSWORD);
    }

    private String other() throws Exception {
        return ApiSignIn.bearer(mvc, "na.other@example.lk", PASSWORD);
    }

    private void insertUser(String id, String role, String email) {
        jdbc.update("""
                INSERT INTO users (id, name, role, email, secret_hash, active)
                VALUES (?, ?, ?, ?, ?, true)""", id, "Test " + role, role, email, encoder.encode(PASSWORD));
    }

    private void insertNotification(String id, String userId, String severity, String title, String createdAt,
            String readAt) {
        jdbc.update("""
                INSERT INTO notifications (id, user_id, severity, type, title, body, created_at, read_at)
                VALUES (?, ?, ?, 'TEST', ?, 'Body', ?::timestamptz, ?::timestamptz)""",
                id, userId, severity, title, createdAt, readAt);
    }
}
