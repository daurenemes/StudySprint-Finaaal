package app.ui;

import app.model.StudyProgress;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.*;

final class Dashboard extends JPanel {
    private final JLabel level = PlannerTheme.label("", 32, true);
    private final JLabel xp = PlannerTheme.label("", 13, false);
    private final JLabel completed = PlannerTheme.label("", 32, true);
    private final JLabel focus = PlannerTheme.label("", 32, true);
    private final JLabel streak = PlannerTheme.label("", 32, true);
    private final JLabel questStatus = PlannerTheme.label("", 28, true);
    private final JLabel questSteps = PlannerTheme.label("", 14, false);
    private final JProgressBar levelProgress = new JProgressBar(0, 100);
    private final WeeklyChart chart = new WeeklyChart();
    private final MissionOrbit orbit = new MissionOrbit();

    Dashboard(Runnable tasks, Runnable timer, Runnable map) {
        super(new BorderLayout(0, 14));
        setOpaque(false);

        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setOpaque(false);
        metrics.add(metric("Level", level, levelProgress));
        metrics.add(metric("Tasks complete", completed, PlannerTheme.label("20 XP per task", 12, false)));
        metrics.add(metric("Timed focus", focus, PlannerTheme.label("15 XP per timer", 12, false)));
        metrics.add(metric("Active streak", streak, PlannerTheme.label("Consecutive days", 12, false)));

        JPanel mission = PlannerTheme.card(new BorderLayout(12, 0));
        JPanel details = new JPanel(new GridLayout(0, 1, 0, 6));
        details.setOpaque(false);
        details.add(PlannerTheme.label("Daily Study Quest", 15, true));
        details.add(questStatus);
        details.add(questSteps);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 2));
        actions.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        actions.setOpaque(false);
        JButton start = PlannerTheme.button("Open Focus Studio", timer);
        start.putClientProperty("primary", true);
        actions.add(start);
        actions.add(PlannerTheme.button("Open tasks", tasks));
        actions.add(PlannerTheme.button("Course Map", map));
        details.add(actions);
        mission.add(details, BorderLayout.CENTER);
        mission.add(orbit, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.add(mission, BorderLayout.NORTH);
        top.add(metrics, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JPanel weekly = PlannerTheme.card(new BorderLayout(0, 12));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel labels = new JPanel(new GridLayout(0, 1, 0, 4));
        labels.setOpaque(false);
        labels.add(PlannerTheme.label("Weekly activity", 23, true));
        labels.add(PlannerTheme.label("Last 7 days", 13, false));
        heading.add(labels, BorderLayout.WEST);
        JComboBox<String> series = new JComboBox<>(new String[]{"XP earned", "Focus minutes"});
        series.setPreferredSize(new Dimension(142, 32));
        series.addActionListener(event -> {
            chart.focusMinutes = series.getSelectedIndex() == 1;
            chart.repaint();
        });
        JPanel selector = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 2));
        selector.setOpaque(false);
        selector.add(series);
        heading.add(selector, BorderLayout.EAST);
        weekly.add(heading, BorderLayout.NORTH);
        weekly.add(chart, BorderLayout.CENTER);
        add(weekly, BorderLayout.CENTER);
    }

    void refresh(StudyProgress progress) {
        level.setText(Integer.toString(progress.level()));
        xp.setText(progress.xp() + " XP total");
        completed.setText(Integer.toString(progress.completedTasks()));
        focus.setText(progress.focusMinutes() + " min");
        streak.setText(progress.streak() + (progress.streak() == 1 ? " day" : " days"));
        levelProgress.setValue(progress.xp() % 100);
        levelProgress.setToolTipText((100 - progress.xp() % 100) + " XP to next level");
        StudyProgress.Day today = progress.today();
        questStatus.setText(today.questComplete() ? "Mission complete" : "One task. One focus session.");
        questSteps.setText("Task  " + Math.min(1, today.tasks()) + "/1     ·     Focus  "
            + Math.min(1, today.sessions()) + "/1     ·     " + (today.questComplete() ? "+10 XP earned" : "Reward  +10 XP"));
        orbit.steps = Math.min(1, today.tasks()) + Math.min(1, today.sessions());
        orbit.repaint();
        chart.progress = progress;
        chart.repaint();
    }

    private JPanel metric(String title, JLabel number, JComponent detail) {
        JPanel panel = PlannerTheme.card(new BorderLayout(0, 6));
        panel.add(PlannerTheme.label(title, 13, true), BorderLayout.NORTH);
        panel.add(number, BorderLayout.CENTER);
        if (detail == levelProgress) {
            JPanel bottom = new JPanel(new BorderLayout(0, 5));
            bottom.setOpaque(false);
            bottom.add(xp, BorderLayout.NORTH);
            bottom.add(detail, BorderLayout.SOUTH);
            panel.add(bottom, BorderLayout.SOUTH);
        } else {
            panel.add(detail, BorderLayout.SOUTH);
        }
        return panel;
    }

    private static final class MissionOrbit extends JComponent {
        private int steps;

        MissionOrbit() { setPreferredSize(new Dimension(150, 118)); }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 17;
            if (size < 18) { g.dispose(); return; }
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            g.setColor(PlannerTheme.line);
            g.drawOval(x - 10, y - 10, size + 20, size + 20);
            g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawOval(x, y, size, size);
            if (steps > 0) {
                g.setColor(steps == 2 ? PlannerTheme.mint : PlannerTheme.accent);
                g.drawArc(x, y, size, size, 90, -180 * steps);
            }
            g.setColor(PlannerTheme.text);
            g.setFont(new Font("SansSerif", Font.BOLD, 27));
            centered(g, steps + "/2", getWidth() / 2, getHeight() / 2 + 9);
            g.dispose();
        }
    }

    private static final class WeeklyChart extends JComponent {
        private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE dd", Locale.ENGLISH);
        private StudyProgress progress;
        private boolean focusMinutes;

        private int value(StudyProgress.Day day) { return focusMinutes ? day.minutes() : day.xp(); }

        @Override protected void paintComponent(Graphics graphics) {
            if (progress == null || getHeight() < 55) return;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            int baseline = getHeight() - 34;
            int space = Math.max(1, getWidth() / 7);
            int width = Math.min(68, space / 2);
            int max = Math.max(1, progress.week().stream().mapToInt(this::value).max().orElse(1));
            g.setColor(PlannerTheme.line);
            for (int line = 0; line < 4; line++) {
                int y = baseline - (baseline - 26) * line / 3;
                g.drawLine(0, y, getWidth(), y);
            }
            for (int index = 0; index < progress.week().size(); index++) {
                StudyProgress.Day day = progress.week().get(index);
                int value = value(day);
                int center = space * index + space / 2;
                int height = Math.max(2, (int) ((baseline - 26) * (double) value / max));
                g.setColor(value == 0 ? PlannerTheme.line : index == 6 ? PlannerTheme.mint : PlannerTheme.accent);
                g.fillRoundRect(center - width / 2, baseline - height, width, height, 12, 12);
                g.setColor(PlannerTheme.text);
                centered(g, value + (focusMinutes ? " m" : ""), center, baseline - height - 8);
                g.setColor(PlannerTheme.muted);
                centered(g, day.date().format(DAY), center, baseline + 24);
            }
            g.dispose();
        }
    }

    private static void centered(Graphics2D g, String text, int x, int y) {
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, y);
    }
}
