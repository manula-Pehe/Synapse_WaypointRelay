package com.synapse.waypoint.core.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.support.ApiSignIn;
import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.settings.service.AdjustableDemoClock;

/**
 * Moving the demo clock past a job's time runs the job before the request returns. Not transactional:
 * jobs commit in their own transactions, so the data here is committed and removed again afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.demo.enabled=true",
    "app.jobs.enabled=true",
    "app.jobs.tick-interval=1h"
})
class ClockMoveRunsJobsTests {

    private static final String PASSWORD = "Test-Password-1";
    private static final String OUTLET = "OUT941";
    private static final String DISPATCHER = "usr-j-clock-disp";
    private static final String DEPOT = OrderFixtures.DEPOT;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired OrderRepository orders;
    @Autowired AdjustableDemoClock clock;

    private Instant clockBeforeTest;

    @BeforeEach
    void createData() {
        removeData();
        clockBeforeTest = clock.now();
        OrderFixtures.insertOutlet(jdbc, OUTLET, "Fresh", "Testdistrict");
        jdbc.update("""
                INSERT INTO users (id, name, role, email, secret_hash, active)
                VALUES (?, 'Test dispatcher', 'DISPATCHER', 'clock.dispatch@example.lk', ?, true)""", DISPATCHER,
                encoder.encode(PASSWORD));
        OrderFixtures.save(orders, OUTLET, "Fresh", TemperatureRequirement.AMBIENT, OrderStatus.PREPARED);
        OrderFixtures.save(orders, OUTLET, "Tech", TemperatureRequirement.AMBIENT, OrderStatus.PREPARED);
    }

    @AfterEach
    void restoreClockAndRemoveData() {
        clock.moveTo(clockBeforeTest);
        removeData();
    }

    @Test
    void shouldHaveClosedTheOrdersInTheDatabaseWhenTheMoveReturns() throws Exception {
        moveClockTo("2026-09-30T16:05:00+05:30");

        assertThat(closedAt()).isNotNull();
        assertThat(statusesOf("Fresh")).containsExactly("CONFIRMED");
        assertThat(statusesOf("Tech")).containsExactly("PREPARED");
        assertThat(completedJobs()).contains("job.close-orders.2026-10-01." + DEPOT);
    }

    @Test
    void shouldNotFireJobsWhoseTimeHasNotCome() throws Exception {
        moveClockTo("2026-09-30T15:10:00+05:30");

        assertThat(closedAt()).isNull();
        assertThat(completedJobs()).contains("job.store-reminder.2026-10-01." + DEPOT)
                .doesNotContain("job.close-orders.2026-10-01." + DEPOT);
    }

    @Test
    void shouldNotRunAJobAgainWhenTheClockIsMovedAgain() throws Exception {
        moveClockTo("2026-09-30T16:05:00+05:30");
        Object firstClosedAt = closedAt();

        moveClockTo("2026-09-30T17:00:00+05:30");
        moveClockTo("2026-09-30T15:00:00+05:30");
        moveClockTo("2026-09-30T18:00:00+05:30");

        assertThat(closedAt()).isEqualTo(firstClosedAt);
    }

    private void moveClockTo(String at) throws Exception {
        mvc.perform(post("/api/settings/clock").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"at\":\"" + at + "\"}")
                        .header(HttpHeaders.AUTHORIZATION,
                                ApiSignIn.bearer(mvc, "clock.dispatch@example.lk", PASSWORD)))
                .andExpect(status().isOk());
    }

    private Object closedAt() {
        return jdbc.queryForList(
                "SELECT orders_closed_at FROM order_runs WHERE depot = ? AND run_date = DATE '2026-10-01'",
                Object.class, DEPOT).stream().filter(Objects::nonNull).findFirst().orElse(null);
    }

    private List<String> statusesOf(String brand) {
        return jdbc.queryForList("SELECT status FROM orders WHERE outlet_id = ? AND brand = ?", String.class, OUTLET,
                brand);
    }

    private List<String> completedJobs() {
        return jdbc.queryForList("SELECT key FROM app_settings WHERE key LIKE 'job.%'", String.class);
    }

    private void removeData() {
        jdbc.update("DELETE FROM notifications WHERE user_id = ?", DISPATCHER);
        jdbc.update("DELETE FROM order_events WHERE order_id IN (SELECT id FROM orders WHERE outlet_id = ?)", OUTLET);
        jdbc.update("DELETE FROM orders WHERE outlet_id = ?", OUTLET);
        jdbc.update("DELETE FROM order_runs WHERE depot = ?", DEPOT);
        jdbc.update("DELETE FROM users WHERE id = ?", DISPATCHER);
        jdbc.update("DELETE FROM outlets WHERE id = ?", OUTLET);
        jdbc.update("DELETE FROM app_settings WHERE key LIKE 'job.%'");
    }
}
