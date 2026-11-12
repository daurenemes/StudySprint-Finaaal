package app.patterns.composite;

import java.util.List;

public interface WorkloadNode {
    String key();
    String title();
    String description();
    String planId();
    List<WorkloadNode> children();
    Progress progress();

    record Progress(int totalTasks, int completedTasks, int remainingMinutes) {
        public Progress plus(Progress other) {
            return new Progress(totalTasks + other.totalTasks, completedTasks + other.completedTasks,
                remainingMinutes + other.remainingMinutes);
        }

        public int percentComplete() {
            return totalTasks == 0 ? 0 : (int) (100L * completedTasks / totalTasks);
        }
    }
}
