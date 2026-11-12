package app.patterns.strategy;

import app.model.StudyTask;
import java.util.*;

public final class ShortestTaskStrategy implements SchedulingStrategy {
    @Override public String name() { return "Shortest first"; }
    @Override public List<StudyTask> order(List<StudyTask> tasks) {
        return tasks.stream().filter(t -> !t.completed())
            .sorted(Comparator.comparingInt(StudyTask::estimateMinutes)
                .thenComparing(StudyTask::dueDate).thenComparing(StudyTask::title)).toList();
    }
    @Override public String toString() { return name(); }
}
