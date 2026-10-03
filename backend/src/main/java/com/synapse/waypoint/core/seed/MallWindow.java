package com.synapse.waypoint.core.seed;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/** A mall's delivery window, written in the dataset as {@code "HH:MM-HH:MM"} (empty when there is none). */
record MallWindow(LocalTime open, LocalTime close) {

    private static final String SEPARATOR = "-";
    private static final int PARTS = 2;

    static Optional<MallWindow> parse(Optional<String> text, CsvRow row, String column) {
        return text.map(value -> parseValue(value, row, column));
    }

    private static MallWindow parseValue(String value, CsvRow row, String column) {
        String[] parts = value.split(SEPARATOR);
        if (parts.length != PARTS) {
            throw row.invalid(column, "must look like HH:MM-HH:MM: '" + value + "'");
        }
        try {
            return new MallWindow(LocalTime.parse(parts[0].trim()), LocalTime.parse(parts[1].trim()));
        } catch (DateTimeParseException e) {
            throw row.invalid(column, "must look like HH:MM-HH:MM: '" + value + "'");
        }
    }
}
