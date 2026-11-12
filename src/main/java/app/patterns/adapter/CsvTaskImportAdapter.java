package app.patterns.adapter;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CsvTaskImportAdapter implements TaskImportSource {
    private static final List<String> TASK_HEADER = List.of("title", "minutes", "priority", "due");
    private static final List<String> REPORT_HEADER =
        List.of("type", "id", "title_or_task", "minutes", "priority", "date", "status");
    private static final int MAX_TASKS = 500;

    @Override public List<TaskDraft> read(Path path) throws IOException {
        List<CsvTableReader.Row> rows = CsvTableReader.read(path);
        if (rows.isEmpty()) throw new IOException("The CSV file is empty.");

        List<String> header = rows.get(0).fields().stream()
            .map(value -> value.strip().toLowerCase(Locale.ROOT)).toList();
        boolean report = REPORT_HEADER.equals(header);
        if (!report && !TASK_HEADER.equals(header)) {
            throw new IOException("Use the header title,minutes,priority,due or choose a Study Sprint report CSV.");
        }

        List<TaskDraft> tasks = new ArrayList<>();
        for (CsvTableReader.Row row : rows.subList(1, rows.size())) {
            List<String> fields = row.fields();
            if (fields.size() != header.size()) throw invalid(row, "expected " + header.size() + " columns");
            if (report) {
                switch (fields.get(0)) {
                    case "plan", "session", "summary" -> { continue; }
                    case "task" -> { }
                    default -> throw invalid(row, "unknown report record type");
                }
            }
            if (tasks.size() == MAX_TASKS) throw new IOException("Import up to 500 tasks at a time.");
            int titleColumn = report ? 2 : 0;
            tasks.add(toTask(row, titleColumn));
        }
        if (tasks.isEmpty()) throw new IOException("The CSV file contains no tasks.");
        return List.copyOf(tasks);
    }

    private static TaskDraft toTask(CsvTableReader.Row row, int titleColumn) throws IOException {
        List<String> fields = row.fields();
        String date = fields.get(titleColumn + 3).strip();
        int minutes = wholeNumber(row, fields.get(titleColumn + 1), "minutes");
        int priority = wholeNumber(row, fields.get(titleColumn + 2), "priority");
        if (date.length() != 10) throw invalid(row, "use a date in YYYY-MM-DD format");
        try {
            return new TaskDraft(fields.get(titleColumn).strip(), minutes, priority, LocalDate.parse(date));
        } catch (DateTimeParseException ex) {
            throw invalid(row, "use a valid date in YYYY-MM-DD format");
        }
    }

    private static int wholeNumber(CsvTableReader.Row row, String value, String column) throws IOException {
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException ex) {
            throw invalid(row, column + " must be a whole number");
        }
    }

    private static IOException invalid(CsvTableReader.Row row, String detail) {
        return new IOException("Invalid CSV on line " + row.line() + " (" + detail + ").");
    }
}
