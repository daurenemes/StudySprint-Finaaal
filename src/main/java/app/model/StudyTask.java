package app.model;

import java.time.LocalDate;

public final class StudyTask {
    private final String id;
    private final String planId;
    private final String title;
    private final int estimateMinutes;
    private final int priority;
    private final LocalDate dueDate;
    private final boolean completed;
    private final LocalDate firstCompletion;

    private StudyTask(Builder builder) {
        if (builder.id == null || builder.id.isBlank()) throw new IllegalArgumentException("Task ID is required");
        if (builder.planId == null || builder.planId.isBlank()) throw new IllegalArgumentException("Plan ID is required");
        if (builder.title == null || builder.title.isBlank() || builder.title.trim().length() > 100) throw new IllegalArgumentException("Task title must contain 1-100 characters");
        if (builder.estimateMinutes < 1 || builder.estimateMinutes > 1440) throw new IllegalArgumentException("Estimate must be between 1 and 1440 minutes");
        if (builder.priority < 1 || builder.priority > 5) throw new IllegalArgumentException("Priority must be between 1 and 5");
        if (builder.dueDate == null) throw new IllegalArgumentException("Due date is required");
        id = builder.id;
        planId = builder.planId;
        title = builder.title.trim();
        estimateMinutes = builder.estimateMinutes;
        priority = builder.priority;
        dueDate = builder.dueDate;
        completed = builder.completed;
        firstCompletion = builder.firstCompletion;
    }

    public String id() { return id; }
    public String planId() { return planId; }
    public String title() { return title; }
    public int estimateMinutes() { return estimateMinutes; }
    public int priority() { return priority; }
    public LocalDate dueDate() { return dueDate; }
    public boolean completed() { return completed; }
    public LocalDate firstCompletion() { return firstCompletion; }
    public Builder toBuilder() { return builder(id, planId).title(title).estimateMinutes(estimateMinutes).priority(priority).dueDate(dueDate).completed(completed).firstCompletion(firstCompletion); }
    public static Builder builder(String id, String planId) { return new Builder(id, planId); }
    @Override public String toString() { return title; }

    public static final class Builder {
        private final String id;
        private final String planId;
        private String title;
        private int estimateMinutes = 25;
        private int priority = 3;
        private LocalDate dueDate = LocalDate.now();
        private boolean completed;
        private LocalDate firstCompletion;

        private Builder(String id, String planId) { this.id = id; this.planId = planId; }
        public Builder title(String value) { title = value; return this; }
        public Builder estimateMinutes(int value) { estimateMinutes = value; return this; }
        public Builder priority(int value) { priority = value; return this; }
        public Builder dueDate(LocalDate value) { dueDate = value; return this; }
        public Builder completed(boolean value) { completed = value; return this; }
        public Builder firstCompletion(LocalDate value) { firstCompletion = value; return this; }
        public StudyTask build() { return new StudyTask(this); }
    }
}
