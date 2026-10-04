package com.synapse.waypoint.core.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.synapse.waypoint.common.seed.SeedMarker;
import com.synapse.waypoint.common.seed.SeedRunner;

/** A dataset folder with only outlets.csv: seeding must fail naming the first missing file. */
@SpringBootTest
@TestPropertySource(properties = "app.data-dir=src/test/resources/seed-incomplete")
class SeedMissingFileTests {

    @Autowired SeedRunner runner;
    @Autowired SeedMarker marker;

    @Test
    void shouldFailNamingTheMissingFileAndNotMarkSeeded() {
        assertThatThrownBy(runner::runOnce)
                .isInstanceOf(DatasetException.class)
                .hasMessageContaining("'vehicles.csv' not found");

        assertThat(marker.isSeeded()).isFalse();
    }
}
