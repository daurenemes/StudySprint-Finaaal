package app.service;

import app.model.*;
import app.storage.StudyRepository;
import app.patterns.strategy.SchedulingStrategy;
import app.patterns.observer.*;
import app.patterns.decorator.*;
import app.patterns.factory.ExportService;
import app.patterns.adapter.TaskDraft;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Clock;
import java.util.*;

public final class StudyPlannerFacade {
    private final StudyRepository repository;
    private final StudyEventHub events;
    private Snapshot state;
    private final Clock clock;

    public StudyPlannerFacade(StudyRepository repository, StudyEventHub events) throws IOException {
        this(repository, events, Clock.systemDefaultZone());
    }

    public StudyPlannerFacade(StudyRepository repository, StudyEventHub events, Clock clock) throws IOException {
        this.clock = Objects.requireNonNull(clock);
        this.repository = Objects.requireNonNull(repository);
        this.events = Objects.requireNonNull(events);
        this.state = repository.load();
        validateRelations(this.state);
        repository.save(this.state);
    }

    public synchronized StudyProgress progress() { return StudyProgress.from(state, LocalDate.now(clock)); }

    public StudyEventHub events() { return events; }
    public synchronized List<StudyPlan> plans() { return state.plans(); }
    public synchronized List<StudyTask> tasks(String planId) {
        return state.tasks().stream().filter(t -> planId == null || t.planId().equals(planId)).toList();
    }
    public synchronized List<FocusSession> sessions(String taskId) {
        return state.sessions().stream().filter(s -> taskId == null || s.taskId().equals(taskId)).toList();
    }

    public synchronized StudyPlan createPlan(String title, String goal) throws IOException {
        StudyPlan plan = new StudyPlan(UUID.randomUUID().toString(), title, goal);
        List<StudyPlan> plans = new ArrayList<>(state.plans());
        plans.add(plan);
        commit(new Snapshot(plans, state.tasks(), state.sessions()), "Plan created: " + plan.title());
        return plan;
    }

    public synchronized void updatePlan(String id, String title, String goal) throws IOException {
        StudyPlan updated = new StudyPlan(id, title, goal);
        List<StudyPlan> plans = new ArrayList<>(state.plans());
        int index = findPlan(plans, id);
        plans.set(index, updated);
        commit(new Snapshot(plans, state.tasks(), state.sessions()), "Plan updated: " + updated.title());
    }

    public synchronized void deletePlan(String id) throws IOException {
        List<StudyPlan> plans = new ArrayList<>(state.plans());
        int index = findPlan(plans, id);
        String name = plans.remove(index).title();
        Set<String> deletedTaskIds = new HashSet<>();
        for (StudyTask t : state.tasks()) if (t.planId().equals(id)) deletedTaskIds.add(t.id());
        List<StudyTask> tasks = state.tasks().stream().filter(t -> !t.planId().equals(id)).toList();
        List<FocusSession> sessions = state.sessions().stream().filter(s -> !deletedTaskIds.contains(s.taskId())).toList();
        commit(new Snapshot(plans, tasks, sessions), "Plan deleted: " + name);
    }

    public synchronized StudyTask createTask(String planId, String title, int minutes, int priority, LocalDate due) throws IOException {
        findPlan(state.plans(), planId);
        StudyTask task = buildTask(planId, title, minutes, priority, due);
        List<StudyTask> tasks = new ArrayList<>(state.tasks());
        tasks.add(task);
        commit(new Snapshot(state.plans(), tasks, state.sessions()), "Task created: " + task.title());
        return task;
    }

    public synchronized int importTasks(String planId, List<TaskDraft> drafts) throws IOException {
        findPlan(state.plans(), planId);
        if (drafts.isEmpty()) return 0;
        List<StudyTask> tasks = new ArrayList<>(state.tasks());
        for (TaskDraft draft : drafts) {
            tasks.add(buildTask(planId, draft.title(), draft.minutes(), draft.priority(), draft.dueDate()));
        }
        commit(new Snapshot(state.plans(), tasks, state.sessions()), "Tasks imported: " + drafts.size());
        return drafts.size();
    }

    private static StudyTask buildTask(String planId, String title, int minutes, int priority, LocalDate due) {
        return StudyTask.builder(UUID.randomUUID().toString(), planId).title(title)
            .estimateMinutes(minutes).priority(priority).dueDate(due).build();
    }

    public synchronized void updateTask(String id, String title, int minutes, int priority, LocalDate due) throws IOException {
        List<StudyTask> tasks = new ArrayList<>(state.tasks());
        int index = findTask(tasks, id);
        StudyTask updated = tasks.get(index).toBuilder().title(title).estimateMinutes(minutes).priority(priority).dueDate(due).build();
        tasks.set(index, updated);
        commit(new Snapshot(state.plans(), tasks, state.sessions()), "Task updated: " + updated.title());
    }

    public record Completion(boolean done, LocalDate firstDate) { }
    public record CompletionChange(Completion before, Completion after) { }

    public synchronized CompletionChange toggleTask(String id) throws IOException {
        StudyTask task = state.tasks().get(findTask(state.tasks(), id));
        Completion before = completion(task);
        LocalDate firstDate = before.firstDate();
        if (!before.done() && firstDate == null) firstDate = LocalDate.now(clock);
        Completion after = new Completion(!before.done(), firstDate);
        saveCompletion(id, before, after, after.done() ? "Task completed: " : "Task reopened: ");
        return new CompletionChange(before, after);
    }

    public synchronized void restoreCompletion(String id, Completion expected, Completion restored) throws IOException {
        saveCompletion(id, expected, restored, "Task completion changed: ");
    }

    private void saveCompletion(String id, Completion expected, Completion restored, String message) throws IOException {
        List<StudyTask> tasks = new ArrayList<>(state.tasks());
        int index = findTask(tasks, id);
        StudyTask task = tasks.get(index);
        if (!completion(task).equals(expected)) throw new IllegalArgumentException("Task completion has changed");
        tasks.set(index, task.toBuilder().completed(restored.done()).firstCompletion(restored.firstDate()).build());
        commit(new Snapshot(state.plans(), tasks, state.sessions()), message + task.title());
    }

    private static Completion completion(StudyTask task) {
        return new Completion(task.completed(), task.firstCompletion());
    }

    public synchronized void deleteTask(String id) throws IOException {
        List<StudyTask> tasks = new ArrayList<>(state.tasks());
        int index = findTask(tasks, id);
        String name = tasks.remove(index).title();
        List<FocusSession> sessions = state.sessions().stream().filter(s -> !s.taskId().equals(id)).toList();
        commit(new Snapshot(state.plans(), tasks, sessions), "Task deleted: " + name);
    }

    public synchronized FocusSession recordSession(String taskId, int minutes, LocalDate date) throws IOException {
        return saveSession(new FocusSession(UUID.randomUUID().toString(), taskId, minutes, date));
    }

    public synchronized FocusSession finishFocus(String taskId, FocusTimer timer) throws IOException {
        if (!timer.ready()) throw new IllegalArgumentException("Finish the countdown first");
        return saveSession(new FocusSession(timer.id(), taskId, timer.targetMinutes(), LocalDate.now(clock), true));
    }

    private FocusSession saveSession(FocusSession session) throws IOException {
        findTask(state.tasks(), session.taskId());
        for (FocusSession saved : state.sessions()) {
            if (saved.id().equals(session.id())) return saved;
        }
        List<FocusSession> sessions = new ArrayList<>(state.sessions());
        sessions.add(session);
        commit(new Snapshot(state.plans(), state.tasks(), sessions), "Focus session recorded: " + session.minutes() + " min");
        return session;
    }

    public synchronized void updateSession(String id, int minutes, LocalDate date) throws IOException {
        List<FocusSession> sessions = new ArrayList<>(state.sessions());
        int index = findSession(sessions, id);
        FocusSession previous = sessions.get(index);
        boolean verified = previous.timed() && previous.minutes() == minutes && previous.date().equals(date);
        sessions.set(index, new FocusSession(id, previous.taskId(), minutes, date, verified));
        commit(new Snapshot(state.plans(), state.tasks(), sessions), "Focus session updated");
    }

    public synchronized void deleteSession(String id) throws IOException {
        List<FocusSession> sessions = new ArrayList<>(state.sessions());
        sessions.remove(findSession(sessions, id));
        commit(new Snapshot(state.plans(), state.tasks(), sessions), "Focus session deleted");
    }

    public synchronized List<StudyTask> schedule(String planId, SchedulingStrategy strategy) {
        findPlan(state.plans(), planId);
        return List.copyOf(Objects.requireNonNull(strategy).order(tasks(planId)));
    }

    public synchronized String summary(String planId, boolean progress, boolean focus) {
        StudyPlan plan = state.plans().get(findPlan(state.plans(), planId));
        List<StudyTask> tasks = tasks(planId);
        Set<String> taskIds = new HashSet<>();
        for (StudyTask task : tasks) taskIds.add(task.id());
        List<FocusSession> sessions = state.sessions().stream().filter(s -> taskIds.contains(s.taskId())).toList();
        SummaryComponent summary = new BaseSummary(plan);
        if (progress) summary = new ProgressSummaryDecorator(summary, tasks);
        if (focus) summary = new FocusSummaryDecorator(summary, sessions);
        return summary.render();
    }

    public synchronized void export(String planId, boolean progress, boolean focus,
                                    ExportService exporter, Path output) throws IOException {
        StudyPlan plan = state.plans().get(findPlan(state.plans(), planId));
        List<StudyTask> tasks = tasks(planId);
        Set<String> ids = new HashSet<>();
        for (StudyTask t : tasks) ids.add(t.id());
        List<FocusSession> sessions = state.sessions().stream().filter(s -> ids.contains(s.taskId())).toList();
        Objects.requireNonNull(exporter).export(plan, tasks, sessions, summary(planId, progress, focus), output);
        events.publish("Report exported: " + output.getFileName());
    }

    private void commit(Snapshot next, String message) throws IOException {
        validateRelations(next);
        repository.save(next);
        state = next;
        events.publish(message);
    }

    private static void validateRelations(Snapshot state) {
        Set<String> plans = new HashSet<>();
        Set<String> tasks = new HashSet<>();
        for (StudyPlan p : state.plans()) if (!plans.add(p.id())) throw new IllegalArgumentException("Duplicate plan ID");
        for (StudyTask t : state.tasks()) {
            if (!plans.contains(t.planId())) throw new IllegalArgumentException("Task references missing plan");
            if (!tasks.add(t.id())) throw new IllegalArgumentException("Duplicate task ID");
        }
        Set<String> sessions = new HashSet<>();
        for (FocusSession s : state.sessions()) {
            if (!tasks.contains(s.taskId())) throw new IllegalArgumentException("Session references missing task");
            if (!sessions.add(s.id())) throw new IllegalArgumentException("Duplicate session ID");
        }
    }

    private static int findPlan(List<StudyPlan> plans, String id) {
        for (int i = 0; i < plans.size(); i++) if (plans.get(i).id().equals(id)) return i;
        throw new IllegalArgumentException("Select an existing plan");
    }
    private static int findTask(List<StudyTask> tasks, String id) {
        for (int i = 0; i < tasks.size(); i++) if (tasks.get(i).id().equals(id)) return i;
        throw new IllegalArgumentException("Select an existing task");
    }
    private static int findSession(List<FocusSession> sessions, String id) {
        for (int i = 0; i < sessions.size(); i++) if (sessions.get(i).id().equals(id)) return i;
        throw new IllegalArgumentException("Select an existing session");
    }
}
