package com.synapse.waypoint.core.seed;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Minimal RFC 4180 reader: a header row, quoted fields (commas, doubled quotes and line breaks
 * inside quotes), CRLF or LF line ends and an optional BOM. Blank lines are skipped.
 */
@Component
public class CsvReader {

    private static final char QUOTE = '"';
    private static final char SEPARATOR = ',';
    private static final char BOM = '﻿';

    public List<CsvRow> read(Path file) {
        String fileName = file.getFileName().toString();
        String content;
        try {
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DatasetException("Cannot read dataset file " + file, e);
        }
        return toRows(fileName, parse(content));
    }

    private List<CsvRow> toRows(String fileName, List<Record> records) {
        if (records.isEmpty()) {
            throw new DatasetException(fileName + " is empty (no header row)");
        }
        List<String> header = records.get(0).fields().stream().map(String::trim).toList();
        List<CsvRow> rows = new ArrayList<>();
        for (Record record : records.subList(1, records.size())) {
            if (record.fields().size() != header.size()) {
                throw new DatasetException("%s line %d: expected %d columns but found %d"
                        .formatted(fileName, record.line(), header.size(), record.fields().size()));
            }
            Map<String, String> values = new HashMap<>();
            for (int i = 0; i < header.size(); i++) {
                values.put(header.get(i), record.fields().get(i));
            }
            rows.add(new CsvRow(fileName, record.line(), values));
        }
        return List.copyOf(rows);
    }

    private List<Record> parse(String content) {
        List<Record> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int line = 1;
        int recordLine = 1;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == BOM && i == 0) {
                continue;
            }
            if (quoted) {
                if (c == QUOTE && i + 1 < content.length() && content.charAt(i + 1) == QUOTE) {
                    field.append(QUOTE);
                    i++;
                } else if (c == QUOTE) {
                    quoted = false;
                } else {
                    field.append(c);
                    if (c == '\n') {
                        line++;
                    }
                }
            } else if (c == QUOTE) {
                quoted = true;
            } else if (c == SEPARATOR) {
                fields.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    i++;
                }
                endRecord(records, fields, field, recordLine);
                line++;
                recordLine = line;
            } else {
                field.append(c);
            }
        }
        if (quoted) {
            throw new DatasetException("Unterminated quoted field starting on line " + recordLine);
        }
        endRecord(records, fields, field, recordLine);
        return records;
    }

    private void endRecord(List<Record> records, List<String> fields, StringBuilder field, int line) {
        boolean blankLine = fields.isEmpty() && field.length() == 0;
        if (!blankLine) {
            fields.add(field.toString());
            records.add(new Record(line, List.copyOf(fields)));
        }
        fields.clear();
        field.setLength(0);
    }

    private record Record(int line, List<String> fields) {
    }
}
