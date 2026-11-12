package app.patterns.composite;

import app.model.StudyPlan;
import app.model.StudyTask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CourseMap {
    private CourseMap() { }

    public static WorkloadNode build(List<StudyPlan> plans, List<StudyTask> tasks) {
        Map<String, List<WorkloadNode>> tasksByPlan = new HashMap<>();
        for (StudyTask task : tasks) {
            tasksByPlan.computeIfAbsent(task.planId(), id -> new ArrayList<>()).add(new TaskNode(task));
        }

        List<WorkloadNode> planNodes = new ArrayList<>();
        for (StudyPlan plan : plans) {
            planNodes.add(new Branch("plan:" + plan.id(), plan.title(), plan.goal(), plan.id(),
                tasksByPlan.getOrDefault(plan.id(), List.of())));
        }
        return new Branch("workspace", "All study plans", plans.isEmpty() ? "Create a plan to start mapping your coursework."
            : plans.size() + (plans.size() == 1 ? " study plan" : " study plans"), null, planNodes);
    }

    private record Branch(String key, String title, String description, String planId,
                          List<WorkloadNode> children) implements WorkloadNode {
        private Branch {
            children = List.copyOf(children);
        }

        @Override public Progress progress() {
            Progress total = new Progress(0, 0, 0);
            for (WorkloadNode child : children) {
                total = total.plus(child.progress());
            }
            return total;
        }
    }

    private record TaskNode(StudyTask task) implements WorkloadNode {
        @Override public String key() { return "task:" + task.id(); }
        @Override public String title() { return task.title(); }
        @Override public String planId() { return task.planId(); }
        @Override public List<WorkloadNode> children() { return List.of(); }

        @Override public String description() {
            return "Due " + task.dueDate() + "  ·  Priority " + task.priority()
                + "  ·  " + task.estimateMinutes() + " min estimated";
        }

        @Override public Progress progress() {
            return task.completed() ? new Progress(1, 1, 0) : new Progress(1, 0, task.estimateMinutes());
        }
    }
}
