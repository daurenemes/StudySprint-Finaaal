package app.patterns.adapter;

import java.time.LocalDate;

public record TaskDraft(String title, int minutes, int priority, LocalDate dueDate) { }
