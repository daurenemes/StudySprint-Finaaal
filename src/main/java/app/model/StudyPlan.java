package app.model;

public record StudyPlan(String id, String title, String goal) {
    public StudyPlan {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Plan ID is required");
        if (title == null || title.isBlank() || title.trim().length() > 80) throw new IllegalArgumentException("Plan title must contain 1-80 characters");
        if (goal == null || goal.trim().length() > 240) throw new IllegalArgumentException("Goal must contain at most 240 characters");
        title = title.trim();
        goal = goal.trim();
    }

    @Override
    public String toString() {
        return title;
    }
}
