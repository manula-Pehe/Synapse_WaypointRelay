package com.synapse.waypoint.core.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.seed.SeedRunner;

/** The whole seed (all modules' steps) against the invented CSVs. */
@SpringBootTest
@TestPropertySource(properties = "app.data-dir=src/test/resources/seed")
@Transactional
class SeedRunOnceTests {

    @Autowired SeedRunner runner;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void shouldLoadEverythingAndRecordCompletion() {
        runner.runOnce();

        assertThat(count("outlets")).isEqualTo(3);
        assertThat(count("orders")).isEqualTo(4);
        assertThat(count("vehicle_availability")).isEqualTo(2);
        assertThat(count("users")).isEqualTo(4);
        assertThat(count("app_settings WHERE key = 'seeded_at'")).isEqualTo(1);
    }

    @Test
    void shouldDoNothingTheSecondTime() {
        runner.runOnce();
        jdbc.update("DELETE FROM order_events");
        jdbc.update("DELETE FROM orders");

        runner.runOnce();

        assertThat(count("orders")).isZero();
        assertThat(count("users")).isEqualTo(4);
    }

    private int count(String tableAndFilter) {
        entityManager.flush();
        return jdbc.queryForObject("SELECT count(*) FROM " + tableAndFilter, Integer.class);
    }
}
