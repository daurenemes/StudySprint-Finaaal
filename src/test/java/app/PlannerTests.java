package app;

import app.model.*;
import app.patterns.factory.*;
import app.patterns.adapter.CsvTaskImportAdapter;
import app.patterns.adapter.TaskDraft;
import app.patterns.command.CommandHistory;
import app.patterns.command.ToggleCompletionCommand;
import app.patterns.composite.CourseMap;
import app.patterns.observer.*;
import app.patterns.strategy.*;
import app.service.StudyPlannerFacade;
import app.service.FocusTimer;
import app.storage.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class PlannerTests {
    private static int passed;
    private static int failed;

    private PlannerTests() { }
    public static void main(String[] args) throws Exception {
        run("T01 Demo data is available on first launch", () -> {
            Fixture f = fixture();
            verify(f.facade().plans().size() == 2, "2 demo plans");
            verify(f.facade().tasks(null).size() == 3, "3 demo tasks");
            verify(Files.exists(f.path()), "saved to disk");
        });
        run("T02 New plan and task persist after restart", () -> {
            Fixture f = fixture();
            StudyPlan plan = f.facade().createPlan("New semester", "Plan my coursework");
            f.facade().createTask(plan.id(), "Implement a project", 45, 4, LocalDate.now().plusDays(2));
            StudyPlannerFacade loaded = new StudyPlannerFacade(new PropertiesRepository(f.path()), new StudyEventHub());
            verify(loaded.plans().stream().anyMatch(p -> p.id().equals(plan.id())), "plan loaded");
            verify(loaded.tasks(plan.id()).size() == 1, "task loaded");
        });
        run("T03 Editing plan and task updates their values", () -> {
            Fixture f = fixture();
            StudyPlan plan = f.facade().plans().get(0);
            StudyTask task = f.facade().tasks(plan.id()).get(0);
            f.facade().updatePlan(plan.id(), "Updated plan", "Updated goal");
            f.facade().updateTask(task.id(), "Updated task", 35, 2, LocalDate.now().plusDays(3));
            verify(f.facade().plans().get(0).title().equals("Updated plan"), "plan updated");
            verify(f.facade().tasks(plan.id()).get(0).estimateMinutes() == 35, "task updated");
        });
        run("T04 Strategy switching changes recommended order", () -> {
            Fixture f = fixture();
            StudyPlan plan = f.facade().createPlan("Strategy check", "Compare algorithms");
            StudyTask longTask = f.facade().createTask(plan.id(), "Long urgent", 90, 5, LocalDate.now());
            StudyTask shortTask = f.facade().createTask(plan.id(), "Short normal", 15, 1, LocalDate.now());
            verify(f.facade().schedule(plan.id(), new PriorityStrategy()).get(0).id().equals(longTask.id()), "priority picks urgent");
            verify(f.facade().schedule(plan.id(), new ShortestTaskStrategy()).get(0).id().equals(shortTask.id()), "shortest picks 15min");
        });
        run("T05 Observer publishes a notification on changes", () -> {
            Fixture f = fixture();
            AtomicInteger notices = new AtomicInteger();
            StudyEventListener listener = e -> notices.incrementAndGet();
            f.facade().events().subscribe(listener);
            f.facade().createPlan("Event example", "Observer check");
            verify(notices.get() == 1, "one event delivered");
            f.facade().events().unsubscribe(listener);
            f.facade().createPlan("Another event", "No further updates");
            verify(notices.get() == 1, "unsubscribe works");
        });
        run("T06 Completing task changes decorated report", () -> {
            Fixture f = fixture();
            StudyPlan plan = f.facade().plans().get(0);
            StudyTask task = f.facade().tasks(plan.id()).get(0);
            String before = f.facade().summary(plan.id(), true, false);
            f.facade().toggleTask(task.id());
            String after = f.facade().summary(plan.id(), true, false);
            verify(!before.equals(after), "progress summary changed");
            verify(after.contains("TASK PROGRESS"), "progress decorator used");
            verify(!after.contains("FOCUS SESSIONS"), "optional focus decorator absent");
        });
        run("T07 Recording, editing and deleting focus time", () -> {
            Fixture f = fixture();
            StudyTask task = f.facade().tasks(null).get(0);
            FocusSession session = f.facade().recordSession(task.id(), 30, LocalDate.now());
            f.facade().updateSession(session.id(), 40, LocalDate.now());
            verify(f.facade().sessions(task.id()).get(0).minutes() == 40, "updated minutes");
            f.facade().deleteSession(session.id());
            verify(f.facade().sessions(task.id()).isEmpty(), "session removed");
        });
        run("T08 Both export variants produce readable results", () -> {
            Fixture f = fixture();
            String planId = f.facade().plans().get(0).id();
            Path txt = f.path().getParent().resolve("summary.txt");
            Path csv = f.path().getParent().resolve("summary.csv");
            f.facade().export(planId, true, true, new TextExportService(), txt);
            f.facade().export(planId, true, true, new CsvExportService(), csv);
            verify(Files.readString(txt).contains("TASK PROGRESS"), "TXT summary");
            verify(Files.readString(csv).startsWith("type,id,"), "CSV headers");
        });
        run("T09 Blank plan title is rejected", () -> {
            Fixture f = fixture();
            expectInvalid(() -> f.facade().createPlan("  ", "Goal"));
        });
        run("T10 Zero estimated minutes is rejected", () -> {
            Fixture f = fixture();
            expectInvalid(() -> f.facade().createTask(f.facade().plans().get(0).id(), "Task", 0, 3, LocalDate.now()));
        });
        run("T11 Priority above five is rejected", () -> {
            Fixture f = fixture();
            expectInvalid(() -> f.facade().createTask(f.facade().plans().get(0).id(), "Task", 25, 6, LocalDate.now()));
        });
        run("T12 Focus session with missing task is rejected", () -> {
            Fixture f = fixture();
            expectInvalid(() -> f.facade().recordSession("missing-task", 25, LocalDate.now()));
        });
        run("T13 Deleting a plan removes related tasks and sessions", () -> {
            Fixture f = fixture();
            StudyPlan plan = f.facade().createPlan("Cascade", "Deletion rules");
            StudyTask task = f.facade().createTask(plan.id(), "Temporary task", 10, 3, LocalDate.now());
            f.facade().recordSession(task.id(), 5, LocalDate.now());
            f.facade().deletePlan(plan.id());
            verify(f.facade().tasks(plan.id()).isEmpty(), "tasks removed");
            verify(f.facade().sessions(task.id()).isEmpty(), "sessions removed");
        });
        run("T14 Timer excludes paused time and rejects an early save", () -> {
            Fixture f = fixture();
            AtomicLong ticks = new AtomicLong();
            FocusTimer timer = new FocusTimer(1, ticks::get);
            String taskId = f.facade().tasks(null).get(0).id();
            timer.start();
            ticks.addAndGet(Duration.ofSeconds(30).toNanos());
            timer.pause();
            ticks.addAndGet(Duration.ofMinutes(5).toNanos());
            verify(timer.remainingSeconds() == 30, "paused time excluded");
            expectInvalid(() -> f.facade().finishFocus(taskId, timer));
            verify(f.facade().sessions(taskId).isEmpty(), "early save wrote no session");
            timer.start();
            ticks.addAndGet(Duration.ofSeconds(30).toNanos());
            verify(timer.ready(), "full active minute elapsed");
            verify(f.facade().finishFocus(taskId, timer).timed(), "timer provenance");
        });
        run("T15 Daily quest and level survive restart without duplicate XP", () -> {
            Fixture f = fixture();
            StudyPlannerFacade planner = f.facade();
            String taskId = planner.tasks(null).get(0).id();
            planner.toggleTask(taskId);
            planner.recordSession(taskId, 25, LocalDate.now());
            verify(!planner.progress().today().questComplete(), "manual log does not qualify");
            for (int i = 0; i < 4; i++) {
                FocusTimer timer = readyTimer();
                planner.finishFocus(taskId, timer);
                planner.finishFocus(taskId, timer);
            }
            StudyPlannerFacade loaded = new StudyPlannerFacade(new PropertiesRepository(f.path()), new StudyEventHub());
            StudyProgress progress = loaded.progress();
            verify(progress.today().questComplete() && progress.today().sessions() == 4, "quest and unique saves");
            verify(progress.xp() == 110 && progress.level() == 2, "one quest bonus and level up");
            verify(progress.focusMinutes() == 4 && progress.week().size() == 7, "timed minutes and weekly view");
        });
        run("T16 Reopening and recompleting cannot move the original completion day", () -> {
            Fixture f = fixture();
            Clock monday = Clock.fixed(Instant.parse("2026-05-04T10:00:00Z"), ZoneOffset.UTC);
            Clock tuesday = Clock.offset(monday, Duration.ofDays(1));
            StudyPlannerFacade first = new StudyPlannerFacade(new PropertiesRepository(f.path()), new StudyEventHub(), monday);
            String taskId = first.tasks(null).get(0).id();
            first.toggleTask(taskId);
            StudyPlannerFacade next = new StudyPlannerFacade(new PropertiesRepository(f.path()), new StudyEventHub(), tuesday);
            next.toggleTask(taskId);
            verify(next.progress().streak() == 0, "reopened task removed from progress");
            next.toggleTask(taskId);
            verify(next.progress().today().tasks() == 0 && next.progress().streak() == 1, "original day retained");
            next.finishFocus(taskId, readyTimer());
            verify(next.progress().streak() == 2 && !next.progress().today().questComplete(), "Tuesday only has a timer");
        });
        run("T17 Edited or deleted timer records update progress", () -> {
            Fixture f = fixture();
            StudyPlannerFacade planner = f.facade();
            String taskId = planner.tasks(null).get(0).id();
            FocusSession session = planner.finishFocus(taskId, readyTimer());
            planner.updateSession(session.id(), 30, LocalDate.now());
            verify(!planner.sessions(taskId).get(0).timed() && planner.progress().focusMinutes() == 0,
                "changed timer becomes manual");
            FocusSession second = planner.finishFocus(taskId, readyTimer());
            planner.deleteSession(second.id());
            verify(planner.progress().focusMinutes() == 0, "deleted timer removed");
        });
        run("T18 Version 1 data loads without invented dates or timer history", () -> {
            Path path = Files.createTempDirectory("study-sprint-legacy-").resolve("legacy.properties");
            Files.copy(Path.of("sample-data", "seed.properties"), path);
            StudyPlannerFacade loaded = new StudyPlannerFacade(new PropertiesRepository(path), new StudyEventHub());
            verify(loaded.tasks(null).size() == 3, "old tasks loaded");
            verify(loaded.progress().xp() == 20 && loaded.progress().today().xp() == 0, "legacy completion is undated");
            verify(loaded.sessions(null).stream().noneMatch(FocusSession::timed), "old manual sessions retained");
        });
        run("T19 Undo and redo restore completion date, quest XP and saved state", () -> {
            Fixture f = fixture();
            StudyPlannerFacade planner = f.facade();
            StudyTask task = planner.tasks(null).get(0);
            planner.finishFocus(task.id(), readyTimer());
            CommandHistory history = new CommandHistory();
            history.execute(new ToggleCompletionCommand(planner, task.id()));
            LocalDate completedOn = planner.tasks(task.planId()).get(0).firstCompletion();
            verify(planner.progress().today().questComplete(), "quest awarded after completion");
            planner.updateTask(task.id(), "Updated title", 33, 4, LocalDate.now());
            history.undo();
            StudyTask undone = planner.tasks(task.planId()).get(0);
            verify(!undone.completed() && undone.firstCompletion() == null && undone.estimateMinutes() == 33,
                "undo restores completion only");
            verify(!planner.progress().today().questComplete(), "undo reverses quest bonus");
            history.redo();
            StudyPlannerFacade loaded = new StudyPlannerFacade(new PropertiesRepository(f.path()), new StudyEventHub());
            StudyTask redone = loaded.tasks(task.planId()).get(0);
            verify(redone.completed() && completedOn.equals(redone.firstCompletion()), "redo restored original date");
            verify(loaded.progress().today().questComplete(), "quest survived reload");
            history.undo();
            history.execute(new ToggleCompletionCommand(planner, planner.tasks(task.planId()).get(1).id()));
            verify(!history.canRedo(), "new command clears redo history");
        });
        run("T20 CSV adapter imports quoted tasks and exported task rows", () -> {
            Fixture f = fixture();
            StudyPlannerFacade planner = f.facade();
            String planId = planner.createPlan("Imported tasks", "Practice").id();
            CsvTaskImportAdapter adapter = new CsvTaskImportAdapter();
            AtomicInteger notifications = new AtomicInteger();
            planner.events().subscribe(event -> notifications.incrementAndGet());
            List<TaskDraft> drafts = adapter.read(Path.of("sample-data", "tasks-import.csv"));
            verify(planner.importTasks(planId, drafts) == 3 && notifications.get() == 1, "single bulk commit");
            verify(planner.tasks(planId).get(0).title().equals("Review graphs, trees and heaps"), "comma preserved");
            Path report = f.path().resolveSibling("report.csv");
            planner.export(planId, true, true, new CsvExportService(), report);
            verify(adapter.read(report).equals(drafts), "report tasks round trip");
            StudyPlannerFacade loaded = new StudyPlannerFacade(new PropertiesRepository(f.path()), new StudyEventHub());
            verify(loaded.tasks(planId).stream().noneMatch(StudyTask::completed), "new tasks saved as unfinished");
        });
        run("T21 Invalid batch is atomic and malformed CSV identifies the row", () -> {
            Fixture f = fixture();
            StudyPlannerFacade planner = f.facade();
            String planId = planner.plans().get(0).id();
            int before = planner.tasks(planId).size();
            AtomicInteger notifications = new AtomicInteger();
            planner.events().subscribe(event -> notifications.incrementAndGet());
            expectInvalid(() -> planner.importTasks(planId, List.of(
                new TaskDraft("Valid", 20, 1, LocalDate.now()),
                new TaskDraft("Invalid", 20, 6, LocalDate.now()))));
            verify(planner.tasks(planId).size() == before && notifications.get() == 0, "no partial imports");
            Path csv = f.path().resolveSibling("malformed.csv");
            Files.writeString(csv, "title,minutes,priority,due\nBroken,20,1,2026-02-30\n");
            try {
                new CsvTaskImportAdapter().read(csv);
                throw new AssertionError("malformed date was accepted");
            } catch (IOException ex) {
                verify(ex.getMessage().contains("line 2"), "reports offending row");
            }
        });
        run("T22 Course Map totals combine plan and task nodes", () -> {
            Fixture f = fixture();
            StudyPlannerFacade planner = f.facade();
            var map = CourseMap.build(planner.plans(), planner.tasks(null));
            verify(map.progress().totalTasks() == 3 && map.progress().completedTasks() == 1, "workspace totals");
            verify(map.progress().remainingMinutes() == 105, "completed task contributes zero remaining minutes");
            var firstPlan = map.children().get(0);
            verify(firstPlan.progress().remainingMinutes() == 105 && firstPlan.children().size() == 2, "plan totals");
            planner.toggleTask(planner.tasks(null).get(0).id());
            var updated = CourseMap.build(planner.plans(), planner.tasks(null));
            verify(updated.progress().remainingMinutes() == 65 && updated.progress().completedTasks() == 2,
                "updated workspace totals");
            verify(CourseMap.build(List.of(), List.of()).progress().percentComplete() == 0, "empty map");
        });
        run("T23 Finished timer state is terminal and idle pause is harmless", () -> {
            AtomicLong ticks = new AtomicLong();
            FocusTimer timer = new FocusTimer(1, ticks::get);
            timer.pause();
            ticks.set(Duration.ofMinutes(2).toNanos());
            verify(!timer.running() && timer.remainingSeconds() == 60, "idle does not advance");
            timer.start();
            ticks.addAndGet(Duration.ofMinutes(1).toNanos());
            timer.pause();
            verify(timer.ready(), "pause at zero transitions to finished");
            timer.start();
            timer.pause();
            verify(timer.ready() && !timer.running() && timer.remainingSeconds() == 0, "finished remains terminal");
        });
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static FocusTimer readyTimer() {
        AtomicLong ticks = new AtomicLong();
        FocusTimer timer = new FocusTimer(1, ticks::get);
        timer.start();
        ticks.set(Duration.ofMinutes(1).toNanos());
        return timer;
    }

    private static Fixture fixture() throws IOException {
        Path path = Files.createTempDirectory("study-sprint-tests-").resolve("data.properties");
        return new Fixture(path, new StudyPlannerFacade(new PropertiesRepository(path), new StudyEventHub()));
    }
    private static void run(String name, Case test) {
        try {
            test.check();
            passed++;
            System.out.println("PASS | " + name + " | actual: as expected");
        } catch (Throwable ex) {
            failed++;
            System.out.println("FAIL | " + name + " | actual: " + ex);
        }
    }
    private static void verify(boolean assertion, String message) {
        if (!assertion) throw new AssertionError(message);
    }
    private static void expectInvalid(Case operation) throws Exception {
        boolean invalid = false;
        try { operation.check(); } catch (IllegalArgumentException ex) { invalid = true; }
        verify(invalid, "expected IllegalArgumentException");
    }
    @FunctionalInterface private interface Case { void check() throws Exception; }
    private record Fixture(Path path, StudyPlannerFacade facade) { }
}
