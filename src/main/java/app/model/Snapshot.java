package app.model;

import java.util.List;

public record Snapshot(List<StudyPlan> plans, List<StudyTask> tasks, List<FocusSession> sessions) {
    public Snapshot {
        plans = List.copyOf(plans);
        tasks = List.copyOf(tasks);
        sessions = List.copyOf(sessions);
    }
}
