package app.ui;

import app.model.StudyPlan;
import app.model.StudyTask;
import app.patterns.composite.CourseMap;
import app.patterns.composite.WorkloadNode;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.text.View;
import javax.swing.tree.*;

final class CourseMapPanel extends JPanel {
    private static final int CARD_WIDTH = 340;
    private static final int CARD_CONTENT_WIDTH = 300;
    private final JTree tree = new JTree(new DefaultMutableTreeNode()) {
        @Override public void updateUI() {
            super.updateUI();
            setBackground(new ColorUIResource(PlannerTheme.surface));
        }

        @Override public String convertValueToText(Object value, boolean selected, boolean expanded,
                                                   boolean leaf, int row, boolean focused) {
            Object item = ((DefaultMutableTreeNode) value).getUserObject();
            return item instanceof WorkloadNode node ? node.title() : "";
        }
    };
    private final JTextArea title = textArea(24, Font.BOLD);
    private final JTextArea description = textArea(14, Font.PLAIN);
    private final JLabel completed = PlannerTheme.label("", 25, true);
    private final JLabel remaining = PlannerTheme.label("", 25, true);
    private final JLabel progressLabel = PlannerTheme.label("", 13, true);
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JButton openTasks;
    private boolean firstRefresh = true;

    CourseMapPanel(Consumer<String> openPlan) {
        super(new BorderLayout(14, 14));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel header = new JPanel(new GridLayout(2, 1, 0, 2));
        header.setOpaque(false);
        header.add(PlannerTheme.label("Course map", 30, true));
        header.add(PlannerTheme.label("Open a study plan to see its tasks and estimated workload.", 14, false));
        add(header, BorderLayout.NORTH);

        tree.setRowHeight(39);
        tree.setShowsRootHandles(true);
        tree.putClientProperty("JTree.lineStyle", "None");
        tree.setCellRenderer(new MapRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.getAccessibleContext().setAccessibleName("Study plan and task hierarchy");
        tree.addTreeSelectionListener(event -> showSelection());
        JScrollPane scroll = new JScrollPane(tree);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        JPanel map = PlannerTheme.card(new BorderLayout(0, 10));
        map.add(PlannerTheme.label("Plans and tasks", 16, true), BorderLayout.NORTH);
        map.add(scroll, BorderLayout.CENTER);
        add(map, BorderLayout.CENTER);

        openTasks = PlannerTheme.button("Open plan tasks", () -> {
            WorkloadNode node = selectedNode();
            if (node != null && node.planId() != null) openPlan.accept(node.planId());
        });
        openTasks.putClientProperty("primary", true);
        add(workloadCard(), BorderLayout.EAST);
    }

    void refresh(List<StudyPlan> plans, List<StudyTask> tasks) {
        WorkloadNode selection = selectedNode();
        Set<String> expanded = new HashSet<>();
        if (!firstRefresh) {
            TreePath oldRoot = new TreePath(tree.getModel().getRoot());
            var paths = tree.getExpandedDescendants(oldRoot);
            if (paths != null) {
                while (paths.hasMoreElements()) expanded.add(nodeAt(paths.nextElement()).key());
            }
        }

        WorkloadNode course = CourseMap.build(plans, tasks);
        Map<String, TreePath> paths = new HashMap<>();
        DefaultMutableTreeNode root = buildTree(course, null, paths);
        tree.setModel(new DefaultTreeModel(root));
        TreePath rootPath = paths.get(course.key());
        tree.expandPath(rootPath);
        for (String key : expanded) {
            TreePath path = paths.get(key);
            if (path != null) tree.expandPath(path);
        }
        if (firstRefresh && !course.children().isEmpty()) tree.expandPath(paths.get(course.children().get(0).key()));
        firstRefresh = false;
        TreePath selectedPath = selection == null ? rootPath : paths.getOrDefault(selection.key(), rootPath);
        tree.setSelectionPath(selectedPath);
        showSelection();
    }

    private JPanel workloadCard() {
        JPanel card = PlannerTheme.card(new BorderLayout(0, 16));
        card.setPreferredSize(new Dimension(CARD_WIDTH, 0));
        JPanel summary = new JPanel(new GridBagLayout());
        summary.setOpaque(false);
        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0;
        layout.weightx = 1;
        layout.fill = GridBagConstraints.HORIZONTAL;
        layout.insets = new Insets(0, 0, 10, 0);
        summary.add(PlannerTheme.label("Workload", 16, true), layout);
        layout.gridy = 1;
        summary.add(title, layout);
        layout.gridy = 2;
        summary.add(description, layout);

        JPanel figures = new JPanel(new GridLayout(1, 2, 8, 0));
        figures.setOpaque(false);
        figures.add(figure("Completed", completed));
        figures.add(figure("Est. remaining", remaining));
        layout.gridy = 3;
        summary.add(figures, layout);
        layout.gridy = 4;
        summary.add(progressLabel, layout);
        layout.gridy = 5;
        progress.setPreferredSize(new Dimension(150, 9));
        progress.getAccessibleContext().setAccessibleName("Completion percentage");
        summary.add(progress, layout);
        card.add(summary, BorderLayout.NORTH);
        card.add(openTasks, BorderLayout.SOUTH);
        return card;
    }

    private static JPanel figure(String label, JLabel value) {
        JPanel box = PlannerTheme.card(new BorderLayout(0, 4));
        box.setBorder(BorderFactory.createEmptyBorder(13, 12, 13, 12));
        box.add(PlannerTheme.label(label, 12, true), BorderLayout.NORTH);
        box.add(value, BorderLayout.CENTER);
        return box;
    }

    private static JTextArea textArea(int size, int style) {
        JTextArea area = new JTextArea();
        area.setFont(new Font(Font.SANS_SERIF, style, size));
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(null);
        return area;
    }

    private static void setContent(JTextArea area, String value) {
        area.setText(value);
        View view = area.getUI().getRootView(area);
        view.setSize(CARD_CONTENT_WIDTH, Short.MAX_VALUE);
        area.setPreferredSize(new Dimension(CARD_CONTENT_WIDTH, (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS))));
        area.setToolTipText(value);
    }

    private void showSelection() {
        WorkloadNode node = selectedNode();
        if (node == null) return;
        WorkloadNode.Progress work = node.progress();
        setContent(title, node.title());
        setContent(description, node.description().isBlank() ? "No study goal set." : node.description());
        completed.setText(work.completedTasks() + "/" + work.totalTasks());
        remaining.setText(duration(work.remainingMinutes()));
        progress.setValue(work.percentComplete());
        progressLabel.setText(work.percentComplete() + "% complete");
        openTasks.setEnabled(node.planId() != null);
    }

    private static String duration(int minutes) {
        if (minutes < 60) return minutes + " min";
        int hours = minutes / 60;
        int remainder = minutes % 60;
        return hours + "h" + (remainder == 0 ? "" : " " + remainder + "m");
    }

    private WorkloadNode selectedNode() {
        TreePath path = tree.getSelectionPath();
        return path == null ? null : nodeAt(path);
    }

    private static WorkloadNode nodeAt(TreePath path) {
        return (WorkloadNode) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
    }

    private static DefaultMutableTreeNode buildTree(WorkloadNode node, TreePath parent, Map<String, TreePath> paths) {
        DefaultMutableTreeNode entry = new DefaultMutableTreeNode(node, !node.key().startsWith("task:"));
        TreePath path = parent == null ? new TreePath(entry) : parent.pathByAddingChild(entry);
        paths.put(node.key(), path);
        for (WorkloadNode child : node.children()) entry.add(buildTree(child, path, paths));
        return entry;
    }

    private static final class MapRenderer extends DefaultTreeCellRenderer {
        @Override public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                               boolean expanded, boolean leaf, int row, boolean focused) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, focused);
            Object item = ((DefaultMutableTreeNode) value).getUserObject();
            if (item instanceof WorkloadNode node) {
                boolean task = node.key().startsWith("task:");
                setFont(tree.getFont().deriveFont(task ? Font.PLAIN : Font.BOLD, 15f));
                setIcon(new NodeIcon(task, task && node.progress().completedTasks() == 1));
                setIconTextGap(10);
            }
            setBackgroundNonSelectionColor(PlannerTheme.surface);
            setBackgroundSelectionColor(PlannerTheme.line);
            setBorderSelectionColor(PlannerTheme.accent);
            setForeground(PlannerTheme.text);
            return this;
        }
    }

    private record NodeIcon(boolean task, boolean done) implements Icon {
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }

        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.translate(x, y);
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pen.setColor(done ? PlannerTheme.mint : PlannerTheme.accent);
            pen.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (task) {
                pen.drawOval(2, 2, 14, 14);
                if (done) {
                    pen.draw(new Line2D.Double(5.5, 9.5, 8, 12));
                    pen.draw(new Line2D.Double(8, 12, 13, 6.5));
                }
            } else {
                pen.drawRoundRect(2, 4, 14, 11, 4, 4);
                pen.drawLine(4, 2, 10, 2);
            }
            pen.dispose();
        }
    }
}
