package com.synapse.waypoint.core.seed;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

/** One data row of a CSV file, read by column name. Errors name the file and line. */
public final class CsvRow {

    private final String fileName;
    private final int lineNumber;
    private final Map<String, String> values;

    CsvRow(String fileName, int lineNumber, Map<String, String> values) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.values = Map.copyOf(values);
    }

    public String text(String column) {
        return optionalText(column)
                .orElseThrow(() -> invalid(column, "is empty"));
    }

    public Optional<String> optionalText(String column) {
        String value = values.get(column);
        if (value == null) {
            throw new DatasetException("%s: column '%s' is missing".formatted(fileName, column));
        }
        return value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    public int integer(String column) {
        String value = text(column);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw invalid(column, "is not a whole number: '" + value + "'");
        }
    }

    public BigDecimal decimal(String column) {
        String value = text(column);
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw invalid(column, "is not a number: '" + value + "'");
        }
    }

    /** Reads {@code 0} / {@code 1} flags. */
    public boolean flag(String column) {
        return switch (text(column)) {
            case "1" -> true;
            case "0" -> false;
            default -> throw invalid(column, "must be 0 or 1");
        };
    }

    public LocalTime time(String column) {
        String value = text(column);
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid(column, "is not a time (HH:MM): '" + value + "'");
        }
    }

    public DatasetException invalid(String column, String problem) {
        return new DatasetException("%s line %d: column '%s' %s".formatted(fileName, lineNumber, column, problem));
    }
}
