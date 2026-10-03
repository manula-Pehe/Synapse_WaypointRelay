package com.synapse.waypoint.core.seed;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Finds dataset files by name anywhere under {@code app.data-dir}. */
@Component
public class DatasetLocator {

    private final Path dataDir;

    DatasetLocator(@Value("${app.data-dir}") Path dataDir) {
        this.dataDir = dataDir;
    }

    public Path locate(String fileName) {
        if (!Files.isDirectory(dataDir)) {
            throw new DatasetException("Dataset folder not found: %s (set APP_DATA_DIR, or SEED_ENABLED=false to skip seeding)"
                    .formatted(dataDir.toAbsolutePath().normalize()));
        }
        List<Path> matches = find(fileName);
        if (matches.isEmpty()) {
            throw new DatasetException("Dataset file '%s' not found under %s"
                    .formatted(fileName, dataDir.toAbsolutePath().normalize()));
        }
        if (matches.size() > 1) {
            throw new DatasetException("Dataset file '%s' found more than once under %s: %s"
                    .formatted(fileName, dataDir.toAbsolutePath().normalize(), matches));
        }
        return matches.get(0);
    }

    private List<Path> find(String fileName) {
        try (Stream<Path> files = Files.walk(dataDir)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase(fileName))
                    .toList();
        } catch (IOException e) {
            throw new DatasetException("Cannot search dataset folder " + dataDir, e);
        }
    }
}
