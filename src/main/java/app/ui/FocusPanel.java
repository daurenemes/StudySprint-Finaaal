package app.ui;

import app.service.FocusTimer;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.Locale;
import javax.swing.*;

final class FocusPanel extends JPanel {
    private final JSpinner duration = new JSpinner(new SpinnerNumberModel(25, 1, 120, 1));
    private final JLabel time = new JLabel("25:00", SwingConstants.CENTER);
    private final TimerDial dial = new TimerDial();
    private final JLabel state = PlannerTheme.label("Choose a task and start", 14, false);
    private final JButton start = PlannerTheme.button("Start", this::startOrPause);
    private final JButton finish = PlannerTheme.button("Finish & save", this::finish);
    private final JButton discard = PlannerTheme.button("Discard", this::discard);
    private final JButton[] presets = {
        PlannerTheme.button("15 min", () -> duration.setValue(15)),
        PlannerTheme.button("25 min", () -> duration.setValue(25)),
        PlannerTheme.button("50 min", () -> duration.setValue(50))
    };
    private final Timer tick = new Timer(200, e -> refresh());
    private final JComboBox<?> tasks;
    private final Saver saver;
    private FocusTimer timer;

    FocusPanel(JComboBox<?> tasks, Saver saver) {
        super(new BorderLayout(0, 8));
        setOpaque(false);
        this.tasks = tasks;
        this.saver = saver;

        JPanel card = PlannerTheme.card(new BorderLayout(24, 0));
        time.setFont(new Font(Font.MONOSPACED, Font.BOLD, 39));
        time.getAccessibleContext().setAccessibleDescription("Remaining focus time");
        dial.add(time);
        card.add(dial, BorderLayout.WEST);

        JPanel controls = new JPanel(new GridBagLayout());
        controls.setOpaque(false);
        Box settings = Box.createVerticalBox();
        settings.add(PlannerTheme.label("Focus timer", 28, true));
        settings.add(Box.createVerticalStrut(5));
        settings.add(state);
        settings.add(Box.createVerticalStrut(18));
        JLabel minutes = PlannerTheme.label("Minutes", 13, true);
        minutes.setLabelFor(duration);
        duration.setPreferredSize(new Dimension(66, 32));
        duration.setMaximumSize(duration.getPreferredSize());
        settings.add(row(minutes, duration, presets[0], presets[1], presets[2]));
        settings.add(Box.createVerticalStrut(10));
        start.putClientProperty("primary", true);
        settings.add(row(start, finish, discard));
        GridBagConstraints position = new GridBagConstraints();
        position.weightx = 1;
        position.anchor = GridBagConstraints.WEST;
        controls.add(settings, position);
        card.add(controls, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
        duration.addChangeListener(e -> refresh());
        tasks.addActionListener(e -> refresh());
        refresh();
    }

    private void startOrPause() {
        if (timer == null) {
            if (tasks.getSelectedItem() == null) return;
            try {
                duration.commitEdit();
            } catch (java.text.ParseException ex) {
                state.setText("Enter a duration from 1 to 120");
                return;
            }
            timer = new FocusTimer((Integer) duration.getValue(), System::nanoTime);
        }
        if (timer.running()) timer.pause();
        else timer.start();
        refresh();
    }

    private void finish() {
        if (timer != null && timer.ready() && saver.save(timer)) {
            discard();
            state.setText("Saved   +15 XP");
        }
    }

    void discard() {
        timer = null;
        refresh();
    }

    boolean active() { return timer != null; }

    private void refresh() {
        boolean active = active();
        long targetSeconds = 60L * (active ? timer.targetMinutes() : (Integer) duration.getValue());
        FocusTimer.Reading reading = active ? timer.reading() : new FocusTimer.Reading(targetSeconds, false);
        long seconds = reading.remainingSeconds();
        boolean ready = active && seconds == 0;
        boolean running = reading.running();
        time.setText(String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60));
        dial.remaining = (double) seconds / targetSeconds;
        dial.repaint();
        state.setText(!active ? "Choose a task and start" : ready ? "Time is up. Save your session." : running ? "Focus in progress" : "Paused");
        start.setText(!active ? "Start" : running ? "Pause" : "Resume");
        start.setEnabled(!ready && tasks.getSelectedItem() != null);
        finish.setEnabled(ready && tasks.getSelectedItem() != null);
        discard.setEnabled(active);
        duration.setEnabled(!active);
        for (JButton preset : presets) preset.setEnabled(!active);
        tasks.setEnabled(!active);
        if (running && !tick.isRunning()) tick.start();
        if (!running) tick.stop();
    }

    private static Box row(JComponent... components) {
        Box row = Box.createHorizontalBox();
        row.setAlignmentX(LEFT_ALIGNMENT);
        for (JComponent component : components) {
            if (row.getComponentCount() > 0) row.add(Box.createHorizontalStrut(8));
            row.add(component);
        }
        return row;
    }

    private static final class TimerDial extends JPanel {
        private double remaining = 1;

        TimerDial() {
            super(new GridBagLayout());
            setOpaque(false);
            setPreferredSize(new Dimension(200, 184));
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double diameter = Math.min(getWidth(), getHeight()) - 18.0;
            double x = (getWidth() - diameter) / 2;
            double y = (getHeight() - diameter) / 2;
            g.setColor(PlannerTheme.background);
            g.fill(new Ellipse2D.Double(x, y, diameter, diameter));
            g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(PlannerTheme.line);
            g.draw(new Ellipse2D.Double(x, y, diameter, diameter));
            if (remaining > 0) {
                g.setPaint(new GradientPaint((float) x, (float) y, PlannerTheme.accent,
                    (float) (x + diameter), (float) (y + diameter), PlannerTheme.mint));
                g.draw(new Arc2D.Double(x, y, diameter, diameter, 90, -360 * remaining, Arc2D.OPEN));
            }
            g.dispose();
        }
    }

    @FunctionalInterface interface Saver { boolean save(FocusTimer timer); }
}
