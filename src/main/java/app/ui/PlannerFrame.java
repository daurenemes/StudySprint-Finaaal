package app.ui;

import app.model.*;
import app.service.StudyPlannerFacade;
import app.service.FocusTimer;
import app.patterns.strategy.*;
import app.patterns.factory.*;
import app.patterns.adapter.CsvTaskImportAdapter;
import app.patterns.command.CommandHistory;
import app.patterns.command.ToggleCompletionCommand;
import java.awt.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public final class PlannerFrame extends JFrame {
    private final StudyPlannerFacade facade;
    private final DefaultListModel<StudyPlan> planModel = new DefaultListModel<>();
    private final JList<StudyPlan> planList = new JList<>(planModel);
    private final JComboBox<StudyPlan> taskPlan = new JComboBox<>();
    private final JComboBox<StudyPlan> reportPlan = new JComboBox<>();
    private final JComboBox<TaskChoice> sessionTask = new JComboBox<>();
    private final JComboBox<SchedulingStrategy> strategy = new JComboBox<>(new SchedulingStrategy[] {
        new PriorityStrategy(), new ShortestTaskStrategy()
    });
    private final DefaultTableModel taskModel = tableModel("Task", "Due", "Priority", "Minutes", "Done");
    private final JTable taskTable = new JTable(taskModel);
    private final DefaultTableModel sessionModel = tableModel("Task", "Minutes", "Date", "Source");
    private final JTable sessionTable = new JTable(sessionModel);
    private final JTextArea schedulePreview = new JTextArea();
    private final JTextArea reportPreview = new JTextArea();
    private final JTextArea activity = new JTextArea();
    private final JCheckBox progressOption = new JCheckBox("Include task progress", true);
    private final JCheckBox focusOption = new JCheckBox("Include focus statistics", true);
    private final JLabel status = new JLabel("Ready");
    private final JLabel metrics = new JLabel(" ");
    private final List<StudyTask> visibleTasks = new ArrayList<>();
    private final List<FocusSession> visibleSessions = new ArrayList<>();
    private static final String[] SCREENS = {"Overview", "Plans", "Tasks & Schedule", "Focus Studio",
        "Reports & Export", "Activity", "Course Map"};
    private final CardLayout screenLayout = new CardLayout();
    private final JPanel screens = new JPanel(screenLayout);
    private final List<JButton> navigation = new ArrayList<>();
    private final CommandHistory history = new CommandHistory();
    private final JButton undo = action("Undo", () -> changeHistory(false));
    private final JButton redo = action("Redo", () -> changeHistory(true));
    private final Dashboard dashboard = new Dashboard(() -> selectTab(2), () -> selectTab(3), () -> selectTab(6));
    private final CourseMapPanel courseMap = new CourseMapPanel(this::openMappedPlan);
    private final FocusPanel focusPanel = new FocusPanel(sessionTask, this::saveTimer);
    private JPanel root;
    private JLabel subtitle;
    private JButton theme;
    private boolean refreshing;

    public PlannerFrame(StudyPlannerFacade facade) {
        super("Study Sprint | Quest Studio");
        this.facade = facade;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Rectangle desktop = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1340, desktop.width), Math.min(850, desktop.height));
        setMinimumSize(new Dimension(1000, 710));
        setLocationRelativeTo(null);
        setContentPane(buildRoot());
        facade.events().subscribe(event -> SwingUtilities.invokeLater(() -> {
            status.setText(event.message());
            activity.insert(event.occurredAt().toLocalTime().withNano(0) + "   " + event.message() + "\n", 0);
            refresh();
        }));
        refresh();
        javax.swing.Timer calendarRefresh = new javax.swing.Timer(30_000, e -> dashboard.refresh(facade.progress()));
        calendarRefresh.start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent event) {
                calendarRefresh.stop();
                focusPanel.discard();
            }
        });
    }

    public void selectTab(int index) {
        screenLayout.show(screens, SCREENS[index]);
        for (int i = 0; i < navigation.size(); i++) {
            JButton button = navigation.get(i);
            button.putClientProperty("primary", i == index);
            button.repaint();
        }
    }

    private void openMappedPlan(String planId) {
        selectPlan(taskPlan, planId);
        selectTab(2);
    }

    private void applyTheme() {
        root.setBackground(PlannerTheme.background);
        subtitle.setForeground(PlannerTheme.muted);
        metrics.setForeground(PlannerTheme.accent);
        status.setForeground(PlannerTheme.muted);
        theme.setText(PlannerTheme.dark() ? "Light theme" : "Dark theme");
        root.repaint();
    }

    private boolean saveTimer(FocusTimer timer) {
        TaskChoice task = (TaskChoice) sessionTask.getSelectedItem();
        return task != null && execute(() -> facade.finishFocus(task.task().id(), timer));
    }

    private JPanel buildRoot() {
        root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(18, 22, 14, 22));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("STUDY SPRINT");
        title.setFont(new Font("SansSerif", Font.BOLD, 29));
        subtitle = new JLabel("Quest Studio");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 1));
        heading.setOpaque(false);
        heading.add(title);
        heading.add(subtitle);
        header.add(heading, BorderLayout.WEST);
        metrics.setFont(new Font("SansSerif", Font.BOLD, 14));
        JPanel headerActions = controls();
        headerActions.add(metrics);
        theme = action("", () -> {
            PlannerTheme.install(!PlannerTheme.dark());
            SwingUtilities.updateComponentTreeUI(this);
            applyTheme();
        });
        headerActions.add(theme);
        header.add(headerActions, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JPanel rail = PlannerTheme.card(new GridBagLayout());
        rail.setBorder(new EmptyBorder(14, 10, 14, 10));
        GridBagConstraints row = new GridBagConstraints();
        row.gridx = 0;
        row.weightx = 1;
        row.fill = GridBagConstraints.HORIZONTAL;
        row.insets = new Insets(0, 0, 6, 0);
        List<JPanel> pages = List.of(dashboard, buildPlans(), buildTasks(), buildSessions(),
            buildReports(), buildActivity(), courseMap);
        for (int index = 0; index < SCREENS.length; index++) {
            final int destination = index;
            String name = SCREENS[index];
            screens.add(pages.get(index), name);
            JButton button = action(name, () -> selectTab(destination));
            button.putClientProperty("navigation", true);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setPreferredSize(new Dimension(158, 40));
            rail.add(button, row);
            navigation.add(button);
        }
        row.weighty = 1;
        row.fill = GridBagConstraints.BOTH;
        rail.add(Box.createGlue(), row);
        screens.setOpaque(false);
        JPanel workspace = new JPanel(new BorderLayout(16, 0));
        workspace.setOpaque(false);
        workspace.add(rail, BorderLayout.WEST);
        workspace.add(screens, BorderLayout.CENTER);
        root.add(workspace, BorderLayout.CENTER);
        selectTab(0);
        status.setBorder(new EmptyBorder(8, 4, 0, 4));
        root.add(status, BorderLayout.SOUTH);
        applyTheme();
        return root;
    }

    private JPanel buildPlans() {
        JPanel panel = canvas();
        JLabel heading = section("Your study plans");
        panel.add(heading, BorderLayout.NORTH);
        DefaultListCellRenderer planRenderer = new DefaultListCellRenderer();
        planRenderer.putClientProperty("html.disable", true);
        planList.setCellRenderer(planRenderer);
        planList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        planList.setFont(new Font("SansSerif", Font.PLAIN, 16));
        planList.setFixedCellHeight(38);
        panel.add(new JScrollPane(planList), BorderLayout.CENTER);
        JPanel buttons = controls();
        buttons.add(action("New plan", () -> editPlan(false)));
        buttons.add(action("Edit plan", () -> editPlan(true)));
        buttons.add(action("Delete plan", this::deletePlan));
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildTasks() {
        JPanel panel = canvas();
        JPanel top = new JPanel(new BorderLayout(10, 12));
        top.setOpaque(false);
        top.add(section("Tasks and scheduling"), BorderLayout.NORTH);
        JPanel choices = controls();
        choices.add(new JLabel("Study plan:"));
        usePlainLabels(taskPlan);
        taskPlan.setPreferredSize(new Dimension(210, 32));
        choices.add(taskPlan);
        choices.add(new JLabel("Scheduling strategy:"));
        strategy.setPreferredSize(new Dimension(165, 32));
        choices.add(strategy);
        choices.add(action("Preview order", this::previewSchedule));
        top.add(choices, BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);

        styleTable(taskTable);
        taskTable.getColumnModel().getColumn(0).setPreferredWidth(340);
        taskTable.getColumnModel().getColumn(1).setPreferredWidth(112);
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel taskArea = new JPanel(new BorderLayout(0, 10));
        taskArea.add(new JScrollPane(taskTable), BorderLayout.CENTER);
        JPanel buttons = controls();
        buttons.add(action("Add task", () -> editTask(false)));
        buttons.add(action("Edit task", () -> editTask(true)));
        buttons.add(action("Complete / reopen", this::toggleTask));
        buttons.add(action("Delete", this::deleteTask));
        buttons.add(action("Import CSV", this::importTasks));
        buttons.add(undo);
        buttons.add(redo);
        undo.setToolTipText("Undo the last completion change");
        redo.setToolTipText("Redo a completion change");
        taskArea.add(buttons, BorderLayout.SOUTH);

        schedulePreview.setEditable(false);
        schedulePreview.setFont(new Font("Monospaced", Font.PLAIN, 13));
        schedulePreview.setText("Select a plan and click Preview order.");
        JScrollPane scheduleScroll = new JScrollPane(schedulePreview);
        scheduleScroll.setBorder(BorderFactory.createTitledBorder("Suggested study order"));
        scheduleScroll.setPreferredSize(new Dimension(100, 200));
        panel.add(taskArea, BorderLayout.CENTER);
        panel.add(scheduleScroll, BorderLayout.SOUTH);
        taskPlan.addActionListener(e -> { if (!refreshing) refreshTasks(); });
        strategy.addActionListener(e -> { if (!refreshing && taskPlan.getSelectedItem() != null) previewSchedule(); });
        return panel;
    }

    private JPanel buildSessions() {
        JPanel panel = canvas();
        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(focusPanel, BorderLayout.NORTH);
        JPanel choices = controls();
        choices.add(new JLabel("Task:"));
        usePlainLabels(sessionTask);
        sessionTask.setPreferredSize(new Dimension(430, 32));
        choices.add(sessionTask);
        top.add(choices, BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);
        styleTable(sessionTable);
        sessionTable.getColumnModel().getColumn(0).setPreferredWidth(360);
        sessionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(sessionTable), BorderLayout.CENTER);
        JPanel buttons = controls();
        buttons.add(action("Log manually", () -> editSession(false)));
        buttons.add(action("Edit session", () -> editSession(true)));
        buttons.add(action("Delete session", this::deleteSession));
        panel.add(buttons, BorderLayout.SOUTH);
        sessionTask.addActionListener(e -> { if (!refreshing) refreshSessions(); });
        return panel;
    }

    private JPanel buildReports() {
        JPanel panel = canvas();
        JPanel controls = new JPanel(new GridLayout(2, 1, 0, 10));
        controls.setOpaque(false);
        JPanel first = controls();
        first.add(new JLabel("Report for:"));
        usePlainLabels(reportPlan);
        reportPlan.setPreferredSize(new Dimension(280, 32));
        first.add(reportPlan);
        first.add(progressOption);
        first.add(focusOption);
        JPanel second = controls();
        second.add(action("Refresh preview", this::previewReport));
        second.add(action("Export TXT", () -> export(false)));
        second.add(action("Export CSV", () -> export(true)));
        controls.add(first);
        controls.add(second);
        panel.add(controls, BorderLayout.NORTH);
        reportPreview.setEditable(false);
        reportPreview.setFont(new Font("Monospaced", Font.PLAIN, 14));
        reportPreview.setBorder(new EmptyBorder(14, 15, 14, 15));
        panel.add(new JScrollPane(reportPreview), BorderLayout.CENTER);
        reportPlan.addActionListener(e -> { if (!refreshing) previewReport(); });
        progressOption.addActionListener(e -> previewReport());
        focusOption.addActionListener(e -> previewReport());
        return panel;
    }

    private JPanel buildActivity() {
        JPanel panel = canvas();
        panel.add(section("Activity log"), BorderLayout.NORTH);
        activity.setEditable(false);
        activity.setFont(new Font("Monospaced", Font.PLAIN, 13));
        activity.setText("Changes to plans, tasks and sessions appear here.\n");
        panel.add(new JScrollPane(activity), BorderLayout.CENTER);
        return panel;
    }

    private void editPlan(boolean edit) {
        StudyPlan original = edit ? planList.getSelectedValue() : null;
        if (edit && original == null) { alert("Select a plan first"); return; }
        JTextField title = new JTextField(original == null ? "" : original.title(), 26);
        JTextField goal = new JTextField(original == null ? "" : original.goal(), 26);
        JPanel form = fields("Plan title", title, "Study goal", goal);
        if (!confirm(form, edit ? "Edit study plan" : "Create study plan")) return;
        execute(() -> {
            if (edit) facade.updatePlan(original.id(), title.getText(), goal.getText());
            else facade.createPlan(title.getText(), goal.getText());
        });
    }

    private void deletePlan() {
        StudyPlan plan = planList.getSelectedValue();
        if (plan == null) { alert("Select a plan first"); return; }
        if (!yes("Delete '" + plan.title() + "' and its tasks and sessions?")) return;
        if (execute(() -> facade.deletePlan(plan.id()))) history.clear();
        refreshHistory();
    }

    private void editTask(boolean edit) {
        StudyPlan plan = (StudyPlan) taskPlan.getSelectedItem();
        if (plan == null) { alert("Create a study plan first"); return; }
        StudyTask original = edit ? selectedTask() : null;
        if (edit && original == null) { alert("Select a task first"); return; }
        JTextField title = new JTextField(original == null ? "" : original.title(), 28);
        JTextField minutes = new JTextField(original == null ? "25" : Integer.toString(original.estimateMinutes()));
        JSpinner priority = new JSpinner(new SpinnerNumberModel(original == null ? 3 : original.priority(), 1, 5, 1));
        JTextField due = new JTextField(original == null ? LocalDate.now().plusDays(1).toString() : original.dueDate().toString());
        JPanel form = fields("Task name", title, "Estimate (minutes)", minutes, "Priority (1-5)", priority, "Due date (YYYY-MM-DD)", due);
        if (!confirm(form, edit ? "Edit task" : "Add task to " + plan.title())) return;
        execute(() -> {
            int estimate = parseMinutes(minutes.getText());
            LocalDate date = LocalDate.parse(due.getText().trim());
            if (edit) facade.updateTask(original.id(), title.getText(), estimate, (Integer) priority.getValue(), date);
            else facade.createTask(plan.id(), title.getText(), estimate, (Integer) priority.getValue(), date);
        });
    }

    private void toggleTask() {
        StudyTask task = selectedTask();
        if (task == null) { alert("Select a task first"); return; }
        execute(() -> history.execute(new ToggleCompletionCommand(facade, task.id())));
        refreshHistory();
    }

    private void deleteTask() {
        StudyTask task = selectedTask();
        if (task == null) { alert("Select a task first"); return; }
        if (!yes("Delete task '" + task.title() + "' and its recorded sessions?")) return;
        if (execute(() -> facade.deleteTask(task.id()))) history.clear();
        refreshHistory();
    }

    private void importTasks() {
        StudyPlan plan = (StudyPlan) taskPlan.getSelectedItem();
        if (plan == null) { alert("Create a study plan first"); return; }
        execute(() -> {
            var drafts = TaskImportDialog.choose(this, new CsvTaskImportAdapter());
            if (!drafts.isEmpty()) facade.importTasks(plan.id(), drafts);
        });
    }

    private void changeHistory(boolean redoAction) {
        execute(() -> {
            if (redoAction) history.redo();
            else history.undo();
        });
        refreshHistory();
    }

    private void refreshHistory() {
        undo.setEnabled(history.canUndo());
        redo.setEnabled(history.canRedo());
    }

    private void editSession(boolean edit) {
        TaskChoice task = (TaskChoice) sessionTask.getSelectedItem();
        FocusSession original = edit ? selectedSession() : null;
        if (!edit && task == null) { alert("Create a task first"); return; }
        if (edit && original == null) { alert("Select a session first"); return; }
        JTextField minutes = new JTextField(original == null ? "25" : Integer.toString(original.minutes()));
        JTextField date = new JTextField(original == null ? LocalDate.now().toString() : original.date().toString());
        JPanel form = fields("Focus minutes", minutes, "Date (YYYY-MM-DD)", date);
        if (!confirm(form, edit ? "Edit focus session" : "Record focus session")) return;
        execute(() -> {
            int length = parseMinutes(minutes.getText());
            LocalDate day = LocalDate.parse(date.getText().trim());
            if (edit) facade.updateSession(original.id(), length, day);
            else facade.recordSession(task.task().id(), length, day);
        });
    }

    private void deleteSession() {
        FocusSession session = selectedSession();
        if (session == null) { alert("Select a session first"); return; }
        if (!yes("Delete this focus session?")) return;
        execute(() -> facade.deleteSession(session.id()));
    }

    private void previewSchedule() {
        StudyPlan plan = (StudyPlan) taskPlan.getSelectedItem();
        SchedulingStrategy selected = (SchedulingStrategy) strategy.getSelectedItem();
        if (plan == null || selected == null) { schedulePreview.setText("Choose a plan."); return; }
        List<StudyTask> tasks = facade.schedule(plan.id(), selected);
        StringBuilder out = new StringBuilder(selected.name() + " — " + plan.title() + "\n\n");
        if (tasks.isEmpty()) out.append("There are no unfinished tasks.\n");
        int index = 1;
        for (StudyTask task : tasks) {
            out.append(index++).append(". ").append(task.title()).append("  |  ")
                .append(task.estimateMinutes()).append(" min  |  P").append(task.priority())
                .append("  |  due ").append(task.dueDate()).append('\n');
        }
        schedulePreview.setText(out.toString());
        schedulePreview.setCaretPosition(0);
    }

    private void previewReport() {
        StudyPlan plan = (StudyPlan) reportPlan.getSelectedItem();
        reportPreview.setText(plan == null ? "Create a plan to generate a report." :
            facade.summary(plan.id(), progressOption.isSelected(), focusOption.isSelected()));
        reportPreview.setCaretPosition(0);
    }

    private void export(boolean csv) {
        StudyPlan plan = (StudyPlan) reportPlan.getSelectedItem();
        if (plan == null) { alert("Choose a plan to export"); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export study report");
        chooser.setSelectedFile(new java.io.File("study-sprint-report." + (csv ? "csv" : "txt")));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path output = chooser.getSelectedFile().toPath();
        if (java.nio.file.Files.exists(output) && !yes("Overwrite the existing file?")) return;
        execute(() -> facade.export(plan.id(), progressOption.isSelected(), focusOption.isSelected(),
            csv ? new CsvExportService() : new TextExportService(), output));
    }

    private void refresh() {
        refreshing = true;
        String selectedList = id(planList.getSelectedValue());
        String selectedTaskPlan = id((StudyPlan) taskPlan.getSelectedItem());
        String selectedReportPlan = id((StudyPlan) reportPlan.getSelectedItem());
        String selectedSessionTask = sessionTask.getSelectedItem() instanceof TaskChoice c ? c.task().id() : null;
        List<StudyPlan> plans = facade.plans();
        List<StudyTask> allTasks = facade.tasks(null);
        planModel.clear();
        taskPlan.removeAllItems();
        reportPlan.removeAllItems();
        for (StudyPlan plan : plans) {
            planModel.addElement(plan);
            taskPlan.addItem(plan);
            reportPlan.addItem(plan);
        }
        for (int i = 0; i < plans.size(); i++) if (plans.get(i).id().equals(selectedList)) planList.setSelectedIndex(i);
        if (planList.isSelectionEmpty() && !plans.isEmpty()) planList.setSelectedIndex(0);
        selectPlan(taskPlan, selectedTaskPlan);
        selectPlan(reportPlan, selectedReportPlan);
        sessionTask.removeAllItems();
        for (StudyTask task : allTasks) {
            StudyPlan owner = plans.stream().filter(p -> p.id().equals(task.planId())).findFirst().orElse(null);
            sessionTask.addItem(new TaskChoice(task, owner == null ? "" : owner.title()));
        }
        selectTask(selectedSessionTask);
        if (focusPanel.active() && allTasks.stream().noneMatch(t -> t.id().equals(selectedSessionTask))) {
            focusPanel.discard();
            status.setText("Focus timer discarded because its task was deleted");
        }
        refreshing = false;
        refreshTasks();
        refreshSessions();
        previewReport();
        StudyProgress progress = facade.progress();
        dashboard.refresh(progress);
        courseMap.refresh(plans, allTasks);
        refreshHistory();
        long completed = progress.completedTasks();
        metrics.setText(plans.size() + " plans   •   " + allTasks.size() + " tasks   •   " + completed + " done");
    }

    private void refreshTasks() {
        taskModel.setRowCount(0);
        visibleTasks.clear();
        StudyPlan plan = (StudyPlan) taskPlan.getSelectedItem();
        if (plan != null) {
            for (StudyTask task : facade.tasks(plan.id())) {
                visibleTasks.add(task);
                taskModel.addRow(new Object[] {task.title(), task.dueDate(), task.priority(), task.estimateMinutes(), task.completed() ? "Yes" : "No"});
            }
        }
        previewSchedule();
    }

    private void refreshSessions() {
        sessionModel.setRowCount(0);
        visibleSessions.clear();
        for (FocusSession session : facade.sessions(null)) {
            visibleSessions.add(session);
            String title = facade.tasks(null).stream().filter(t -> t.id().equals(session.taskId())).map(StudyTask::title)
                .findFirst().orElse("Unknown");
            sessionModel.addRow(new Object[] {title, session.minutes(), session.date(), session.timed() ? "Timer" : "Manual"});
        }
    }

    private static String id(StudyPlan p) { return p == null ? null : p.id(); }
    private static void selectPlan(JComboBox<StudyPlan> combo, String id) {
        if (id == null) return;
        for (int i = 0; i < combo.getItemCount(); i++) if (combo.getItemAt(i).id().equals(id)) { combo.setSelectedIndex(i); return; }
    }
    private void selectTask(String id) {
        if (id == null) return;
        for (int i = 0; i < sessionTask.getItemCount(); i++)
            if (sessionTask.getItemAt(i).task().id().equals(id)) { sessionTask.setSelectedIndex(i); return; }
    }
    private StudyTask selectedTask() {
        int index = taskTable.getSelectedRow();
        if (index < 0) return null;
        index = taskTable.convertRowIndexToModel(index);
        return index >= visibleTasks.size() ? null : visibleTasks.get(index);
    }
    private FocusSession selectedSession() {
        int index = sessionTable.getSelectedRow();
        if (index < 0) return null;
        index = sessionTable.convertRowIndexToModel(index);
        return index >= visibleSessions.size() ? null : visibleSessions.get(index);
    }
    private static int parseMinutes(String text) {
        try { return Integer.parseInt(text.trim()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Minutes must be a whole number"); }
    }
    private boolean execute(Operation operation) {
        try { operation.run(); return true; }
        catch (DateTimeParseException ex) { alert("Use the date format YYYY-MM-DD"); }
        catch (Exception ex) { alert(ex.getMessage() == null ? ex.toString() : ex.getMessage()); }
        return false;
    }
    private void alert(String message) {
        status.setText("Error: " + message);
        JOptionPane.showMessageDialog(this, message, "Input / action error", JOptionPane.ERROR_MESSAGE);
    }
    private boolean yes(String message) { return JOptionPane.showConfirmDialog(this, message, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION; }
    private boolean confirm(JPanel fields, String title) { return JOptionPane.showConfirmDialog(this, fields, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION; }
    private static JPanel fields(Object... fields) {
        JPanel panel = new JPanel(new GridLayout(fields.length / 2, 2, 12, 10));
        panel.setBorder(new EmptyBorder(10, 8, 8, 8));
        for (int i = 0; i < fields.length; i += 2) {
            panel.add(new JLabel(fields[i] + ":"));
            panel.add((Component) fields[i + 1]);
        }
        return panel;
    }
    private static JPanel canvas() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBorder(new EmptyBorder(16, 10, 12, 10));
        return panel;
    }
    private static JPanel controls() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        row.setOpaque(false);
        return row;
    }
    private static JButton action(String name, Runnable operation) {
        return PlannerTheme.button(name, operation);
    }
    private static JLabel section(String name) {
        JLabel heading = new JLabel(name);
        heading.setFont(new Font("SansSerif", Font.BOLD, 19));
        return heading;
    }
    private static void usePlainLabels(JComboBox<?> combo) {
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        renderer.putClientProperty("html.disable", true);
        combo.setRenderer(renderer);
    }

    private static void styleTable(JTable table) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.putClientProperty("html.disable", true);
        renderer.setBorder(new EmptyBorder(0, 8, 0, 8));
        table.setDefaultRenderer(Object.class, renderer);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.getTableHeader().setPreferredSize(new Dimension(0, 32));
        table.setRowHeight(34);
    }

    private static DefaultTableModel tableModel(String... headings) {
        return new DefaultTableModel(headings, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }
    private record TaskChoice(StudyTask task, String planTitle) {
        @Override public String toString() { return task.title() + "  (" + planTitle + ")"; }
    }
    @FunctionalInterface private interface Operation { void run() throws Exception; }
}
