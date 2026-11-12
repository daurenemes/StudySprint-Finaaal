package app.patterns.observer;

import java.time.LocalDateTime;

public record StudyChangeEvent(String message, LocalDateTime occurredAt) { }
