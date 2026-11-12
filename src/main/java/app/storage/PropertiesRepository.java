package app.storage;

import app.model.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public final class PropertiesRepository implements StudyRepository {
    private final Path file;

    public PropertiesRepository(Path file) { this.file = Objects.requireNonNull(file); }

    @Override
    public Snapshot load() throws IOException {
        if (!Files.exists(file)) return SampleData.create();
        Properties p = new Properties();
        try (Reader input = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { p.load(input); }
        List<StudyPlan> plans = new ArrayList<>();
        List<StudyTask> tasks = new ArrayList<>();
        List<FocusSession> sessions = new ArrayList<>();
        try {
            int planCount = count(p, "plan.count");
            int taskCount = count(p, "task.count");
            int sessionCount = count(p, "session.count");
            for (int i = 0; i < planCount; i++) {
                String pre = "plan." + i + ".";
                plans.add(new StudyPlan(required(p, pre + "id"), required(p, pre + "title"), required(p, pre + "goal")));
            }
            for (int i = 0; i < taskCount; i++) {
                String pre = "task." + i + ".";
                tasks.add(StudyTask.builder(required(p, pre + "id"), required(p, pre + "planId"))
                    .title(required(p, pre + "title"))
                    .estimateMinutes(Integer.parseInt(required(p, pre + "minutes")))
                    .priority(Integer.parseInt(required(p, pre + "priority")))
                    .dueDate(LocalDate.parse(required(p, pre + "due")))
                    .completed(Boolean.parseBoolean(required(p, pre + "completed")))
                    .firstCompletion(p.containsKey(pre + "firstCompletion")
                        ? LocalDate.parse(required(p, pre + "firstCompletion")) : null).build());
            }
            for (int i = 0; i < sessionCount; i++) {
                String pre = "session." + i + ".";
                sessions.add(new FocusSession(required(p, pre + "id"), required(p, pre + "taskId"),
                    Integer.parseInt(required(p, pre + "minutes")), LocalDate.parse(required(p, pre + "date")),
                    Boolean.parseBoolean(p.getProperty(pre + "timed", "false"))));
            }
        } catch (RuntimeException ex) {
            throw new IOException("The data file is invalid: " + ex.getMessage(), ex);
        }
        return new Snapshot(plans, tasks, sessions);
    }

    private static int count(Properties p, String key) {
        int n = Integer.parseInt(required(p, key));
        if (n < 0 || n > 100000) throw new IllegalArgumentException("Invalid " + key);
        return n;
    }

    private static String required(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null) throw new IllegalArgumentException("Missing " + key);
        return value;
    }

    @Override
    public void save(Snapshot snapshot) throws IOException {
        Properties p = new Properties();
        p.setProperty("format.version", "2");
        p.setProperty("plan.count", Integer.toString(snapshot.plans().size()));
        p.setProperty("task.count", Integer.toString(snapshot.tasks().size()));
        p.setProperty("session.count", Integer.toString(snapshot.sessions().size()));
        for (int i = 0; i < snapshot.plans().size(); i++) {
            StudyPlan item = snapshot.plans().get(i);
            String pre = "plan." + i + ".";
            p.setProperty(pre + "id", item.id());
            p.setProperty(pre + "title", item.title());
            p.setProperty(pre + "goal", item.goal());
        }
        for (int i = 0; i < snapshot.tasks().size(); i++) {
            StudyTask item = snapshot.tasks().get(i);
            String pre = "task." + i + ".";
            p.setProperty(pre + "id", item.id());
            p.setProperty(pre + "planId", item.planId());
            p.setProperty(pre + "title", item.title());
            p.setProperty(pre + "minutes", Integer.toString(item.estimateMinutes()));
            p.setProperty(pre + "priority", Integer.toString(item.priority()));
            p.setProperty(pre + "due", item.dueDate().toString());
            p.setProperty(pre + "completed", Boolean.toString(item.completed()));
            if (item.firstCompletion() != null) p.setProperty(pre + "firstCompletion", item.firstCompletion().toString());
        }
        for (int i = 0; i < snapshot.sessions().size(); i++) {
            FocusSession item = snapshot.sessions().get(i);
            String pre = "session." + i + ".";
            p.setProperty(pre + "id", item.id());
            p.setProperty(pre + "taskId", item.taskId());
            p.setProperty(pre + "minutes", Integer.toString(item.minutes()));
            p.setProperty(pre + "date", item.date().toString());
            p.setProperty(pre + "timed", Boolean.toString(item.timed()));
        }
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        Files.createDirectories(parent);
        Path tmp = Files.createTempFile(parent, "study-sprint-", ".tmp");
        try {
            try (Writer output = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                p.store(output, "Study Sprint data");
            }
            replace(tmp, absolute);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static void replace(Path source, Path target) throws IOException {
        for (int attempt = 0; ; attempt++) {
            try {
                try {
                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            } catch (AccessDeniedException ex) {
                if (attempt == 3) throw ex;
                try {
                    Thread.sleep(30L * (attempt + 1));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
    }
}
