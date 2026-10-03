package com.synapse.waypoint.core.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvReaderTests {

    private final CsvReader reader = new CsvReader();

    @TempDir
    Path folder;

    @Test
    void shouldReadRowsByColumnName() throws IOException {
        List<CsvRow> rows = read("id,name\nA1,First\nA2,Second\n");

        assertThat(rows).hasSize(2);
        assertThat(rows.get(1).text("id")).isEqualTo("A2");
        assertThat(rows.get(1).text("name")).isEqualTo("Second");
    }

    @Test
    void shouldHandleQuotedFieldsWithCommasQuotesAndLineBreaks() throws IOException {
        List<CsvRow> rows = read("id,note\nA1,\"one, two\"\nA2,\"say \"\"hi\"\"\"\nA3,\"line1\nline2\"\n");

        assertThat(rows).extracting(row -> row.text("note"))
                .containsExactly("one, two", "say \"hi\"", "line1\nline2");
    }

    @Test
    void shouldAcceptWindowsLineEndingsBomAndBlankLines() throws IOException {
        List<CsvRow> rows = read("﻿id,name\r\nA1,First\r\n\r\nA2,Second");

        assertThat(rows).extracting(row -> row.text("id")).containsExactly("A1", "A2");
    }

    @Test
    void shouldTreatEmptyFieldAsAbsentOptionalValue() throws IOException {
        CsvRow row = read("id,window\nA1,\n").get(0);

        assertThat(row.optionalText("window")).isEmpty();
        assertThatThrownBy(() -> row.text("window")).hasMessageContaining("line 2").hasMessageContaining("'window'");
    }

    @Test
    void shouldParseTypedValues() throws IOException {
        CsvRow row = read("n,d,f,t\n12,3.50,1,05:30\n").get(0);

        assertThat(row.integer("n")).isEqualTo(12);
        assertThat(row.decimal("d")).isEqualByComparingTo("3.5");
        assertThat(row.flag("f")).isTrue();
        assertThat(row.time("t")).isEqualTo(LocalTime.of(5, 30));
    }

    @Test
    void shouldNameFileLineAndColumnForBadValues() throws IOException {
        CsvRow row = read("n,f\nabc,2\n").get(0);

        assertThatThrownBy(() -> row.integer("n")).hasMessageContaining("sample.csv line 2").hasMessageContaining("'n'");
        assertThatThrownBy(() -> row.flag("f")).hasMessageContaining("must be 0 or 1");
    }

    @Test
    void shouldRejectUnknownColumn() throws IOException {
        CsvRow row = read("id\nA1\n").get(0);

        assertThatThrownBy(() -> row.text("nope")).hasMessageContaining("column 'nope' is missing");
    }

    @Test
    void shouldRejectRowWithWrongNumberOfColumns() {
        assertThatThrownBy(() -> read("a,b\n1,2,3\n")).hasMessageContaining("line 2").hasMessageContaining("expected 2 columns");
    }

    @Test
    void shouldRejectUnterminatedQuote() {
        assertThatThrownBy(() -> read("a\n\"open\n")).hasMessageContaining("Unterminated");
    }

    @Test
    void shouldRejectEmptyFile() {
        assertThatThrownBy(() -> read("")).hasMessageContaining("empty");
    }

    private List<CsvRow> read(String content) throws IOException {
        Path file = folder.resolve("sample.csv");
        Files.writeString(file, content);
        return reader.read(file);
    }
}
