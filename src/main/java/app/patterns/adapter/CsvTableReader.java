package app.patterns.adapter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class CsvTableReader {
    private static final int MAX_BYTES = 1_048_576;
    private static final int MAX_RECORDS = 10_000;
    private static final int MAX_FIELDS = 7;
    private final String text;
    private int cursor;
    private int line = 1;

    private CsvTableReader(String text) {
        this.text = text;
        if (at('\uFEFF')) cursor++;
    }

    static List<Row> read(Path path) throws IOException {
        byte[] bytes;
        try (var input = Files.newInputStream(path)) {
            bytes = input.readNBytes(MAX_BYTES + 1);
        }
        if (bytes.length > MAX_BYTES) throw new IOException("Choose a CSV file of 1 MiB or less.");
        try {
            String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            return new CsvTableReader(text).rows();
        } catch (CharacterCodingException ex) {
            throw new IOException("Save the CSV file as UTF-8 and try again.", ex);
        }
    }

    private List<Row> rows() throws IOException {
        List<Row> rows = new ArrayList<>();
        while (cursor < text.length()) {
            int startLine = line;
            List<String> fields = new ArrayList<>();
            fields.add(field());
            while (at(',')) {
                if (fields.size() == MAX_FIELDS) throw malformed("too many columns");
                cursor++;
                fields.add(field());
            }
            if (cursor < text.length()) {
                if (!atLineBreak()) throw malformed("expected a comma or line break");
                consumeLineBreak();
            }
            if (fields.size() == 1 && fields.get(0).isBlank()) continue;
            if (rows.size() == MAX_RECORDS) throw new IOException("CSV contains more than 10,000 records.");
            rows.add(new Row(startLine, List.copyOf(fields)));
        }
        return rows;
    }

    private String field() throws IOException {
        if (at('"')) {
            cursor++;
            return quotedField();
        }
        int start = cursor;
        while (cursor < text.length() && !at(',') && !atLineBreak()) {
            if (at('"')) throw malformed("quote inside an unquoted field");
            cursor++;
        }
        return text.substring(start, cursor);
    }

    private String quotedField() throws IOException {
        StringBuilder value = new StringBuilder();
        while (cursor < text.length()) {
            if (atLineBreak()) {
                consumeLineBreak();
                value.append('\n');
            } else {
                char character = text.charAt(cursor++);
                if (character == '"') {
                    if (!at('"')) return value.toString();
                    cursor++;
                }
                value.append(character);
            }
        }
        throw malformed("missing closing quote");
    }

    private boolean at(char character) {
        return cursor < text.length() && text.charAt(cursor) == character;
    }

    private boolean atLineBreak() { return at('\r') || at('\n'); }

    private void consumeLineBreak() {
        char first = text.charAt(cursor++);
        if (first == '\r' && at('\n')) cursor++;
        line++;
    }

    private IOException malformed(String detail) {
        return new IOException("Invalid CSV on line " + line + " (" + detail + ").");
    }

    record Row(int line, List<String> fields) { }
}
