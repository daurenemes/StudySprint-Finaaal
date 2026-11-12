package app.model;

import java.time.LocalDate;

public record FocusSession(String id, String taskId, int minutes, LocalDate date, boolean timed) {
    public FocusSession(String id, String taskId, int minutes, LocalDate date) {
        this(id, taskId, minutes, date, false);
    }

    public FocusSession {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Session ID is required");
        if (taskId == null || taskId.isBlank()) throw new IllegalArgumentException("Task ID is required");
        if (minutes < 1 || minutes > 1440) throw new IllegalArgumentException("Focus minutes must be between 1 and 1440");
        if (date == null) throw new IllegalArgumentException("Session date is required");
    }
}
