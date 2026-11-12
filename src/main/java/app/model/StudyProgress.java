package app.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

public record StudyProgress(int xp, int level, int streak, int completedTasks, int focusMinutes,
                            List<Day> week) {
    public StudyProgress {
        week = List.copyOf(week);
    }

    public record Day(LocalDate date, int tasks, int sessions, int minutes) {
        public boolean questComplete() { return tasks > 0 && sessions > 0; }
        public int xp() { return tasks * 20 + sessions * 15 + (questComplete() ? 10 : 0); }
    }

    public Day today() { return week.get(week.size() - 1); }

    public static StudyProgress from(Snapshot snapshot, LocalDate today) {
        NavigableMap<LocalDate, Day> activity = new TreeMap<>();
        int completed = 0;
        int minutes = 0;
        int undatedXp = 0;
        for (StudyTask task : snapshot.tasks()) {
            if (!task.completed()) continue;
            completed++;
            LocalDate date = task.firstCompletion();
            if (date == null) {
                undatedXp += 20;
            } else if (!date.isAfter(today)) {
                Day day = activity.getOrDefault(date, new Day(date, 0, 0, 0));
                activity.put(date, new Day(date, day.tasks + 1, day.sessions, day.minutes));
            }
        }
        for (FocusSession session : snapshot.sessions()) {
            if (!session.timed() || session.date().isAfter(today)) continue;
            LocalDate date = session.date();
            Day day = activity.getOrDefault(date, new Day(date, 0, 0, 0));
            activity.put(date, new Day(date, day.tasks, day.sessions + 1, day.minutes + session.minutes()));
            minutes += session.minutes();
        }
        int xp = undatedXp + activity.values().stream().mapToInt(Day::xp).sum();
        int streak = 0;
        LocalDate cursor = activity.containsKey(today) ? today : today.minusDays(1);
        while (activity.containsKey(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        List<Day> week = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            week.add(activity.getOrDefault(date, new Day(date, 0, 0, 0)));
        }
        return new StudyProgress(xp, 1 + xp / 100, streak, completed, minutes, week);
    }
}
