package app.patterns.strategy;

import app.model.StudyTask;
import java.util.*;

public final class PriorityStrategy implements SchedulingStrategy {
    @Override public String name() { return "Priority first"; }
    @Override public List<StudyTask> order(List<StudyTask> tasks) {
        return tasks.stream().filter(t -> !t.completed())
            .sorted(Comparator.comparingInt(StudyTask::priority).reversed()
                .thenComparing(StudyTask::dueDate).thenComparing(StudyTask::title)).toList();
    }
    @Override public String toString() { return name(); }
}
