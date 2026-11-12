package app.patterns.decorator;

import app.model.StudyTask;
import java.util.List;

public final class ProgressSummaryDecorator extends SummaryDecorator {
    private final List<StudyTask> tasks;
    public ProgressSummaryDecorator(SummaryComponent base, List<StudyTask> tasks) {
        super(base);
        this.tasks = List.copyOf(tasks);
    }
    @Override public String render() {
        long completed = tasks.stream().filter(StudyTask::completed).count();
        int minutes = tasks.stream().mapToInt(StudyTask::estimateMinutes).sum();
        StringBuilder out = new StringBuilder(base.render());
        out.append("\nTASK PROGRESS\nCompleted: ").append(completed).append(" / ").append(tasks.size());
        out.append("\nEstimated study time: ").append(minutes).append(" min\n");
        for (StudyTask t : tasks) {
            out.append(t.completed() ? "[x] " : "[ ] ").append(t.title())
                .append(" | P").append(t.priority()).append(" | ")
                .append(t.estimateMinutes()).append(" min | due ").append(t.dueDate()).append("\n");
        }
        return out.toString();
    }
}
