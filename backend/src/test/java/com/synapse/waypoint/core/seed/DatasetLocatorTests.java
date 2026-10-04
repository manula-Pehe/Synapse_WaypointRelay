package com.synapse.waypoint.core.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatasetLocatorTests {

    @TempDir
    Path folder;

    @Test
    void shouldFindFileInANestedFolder() throws IOException {
        Path expected = create("General Data/sample.csv");

        assertThat(new DatasetLocator(folder).locate("sample.csv")).isEqualTo(expected);
    }

    @Test
    void shouldNameTheMissingFile() {
        assertThatThrownBy(() -> new DatasetLocator(folder).locate("sample.csv"))
                .isInstanceOf(DatasetException.class)
                .hasMessageContaining("'sample.csv' not found");
    }

    @Test
    void shouldExplainAMissingFolder() {
        assertThatThrownBy(() -> new DatasetLocator(folder.resolve("absent")).locate("sample.csv"))
                .hasMessageContaining("Dataset folder not found")
                .hasMessageContaining("SEED_ENABLED=false");
    }

    @Test
    void shouldRejectAmbiguousFile() throws IOException {
        create("one/sample.csv");
        create("two/sample.csv");

        assertThatThrownBy(() -> new DatasetLocator(folder).locate("sample.csv")).hasMessageContaining("more than once");
    }

    private Path create(String relative) throws IOException {
        Path file = folder.resolve(relative);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, "a\n1\n");
    }
}
