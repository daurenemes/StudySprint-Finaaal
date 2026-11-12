package app.patterns.command;

import app.service.StudyPlannerFacade;
import java.io.IOException;

public final class ToggleCompletionCommand implements StudyCommand {
    private final StudyPlannerFacade planner;
    private final String taskId;
    private StudyPlannerFacade.CompletionChange change;

    public ToggleCompletionCommand(StudyPlannerFacade planner, String taskId) {
        this.planner = planner;
        this.taskId = taskId;
    }

    @Override public void execute() throws IOException {
        if (change == null) change = planner.toggleTask(taskId);
        else planner.restoreCompletion(taskId, change.before(), change.after());
    }

    @Override public void undo() throws IOException {
        if (change == null) throw new IllegalStateException("Run the command before undoing it");
        planner.restoreCompletion(taskId, change.after(), change.before());
    }
}
