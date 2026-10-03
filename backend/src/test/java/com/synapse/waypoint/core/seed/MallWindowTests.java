package com.synapse.waypoint.core.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MallWindowTests {

    @TempDir
    Path folder;

    private CsvRow row;

    @BeforeEach
    void setUp() throws IOException {
        Path file = Files.writeString(folder.resolve("outlets.csv"), "mall_window\nx\n");
        row = new CsvReader().read(file).get(0);
    }

    @Test
    void shouldSplitOpenAndCloseTimes() {
        MallWindow window = MallWindow.parse(Optional.of("10:00-12:30"), row, "mall_window").orElseThrow();

        assertThat(window.open()).isEqualTo(LocalTime.of(10, 0));
        assertThat(window.close()).isEqualTo(LocalTime.of(12, 30));
    }

    @Test
    void shouldReturnEmptyWhenOutletHasNoMallWindow() {
        assertThat(MallWindow.parse(Optional.empty(), row, "mall_window")).isEmpty();
    }

    @Test
    void shouldRejectMalformedWindow() {
        assertThatThrownBy(() -> MallWindow.parse(Optional.of("10:00"), row, "mall_window"))
                .hasMessageContaining("HH:MM-HH:MM");
        assertThatThrownBy(() -> MallWindow.parse(Optional.of("ab-cd"), row, "mall_window"))
                .hasMessageContaining("HH:MM-HH:MM");
    }
}
