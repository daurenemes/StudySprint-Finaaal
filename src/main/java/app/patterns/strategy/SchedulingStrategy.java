package app.patterns.strategy;

import app.model.StudyTask;
import java.util.List;

public interface SchedulingStrategy {
    String name();
    List<StudyTask> order(List<StudyTask> tasks);
}
