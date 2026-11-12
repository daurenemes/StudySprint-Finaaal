package app.ui;

import app.patterns.adapter.TaskDraft;
import app.patterns.adapter.TaskImportSource;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public final class TaskImportDialog {
    private TaskImportDialog() { }

    public static List<TaskDraft> choose(Component parent, TaskImportSource source) throws IOException {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a task list or Study Sprint report");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return List.of();

        List<TaskDraft> tasks = source.read(chooser.getSelectedFile().toPath());
        JTable table = previewTable(tasks);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(630, 230));

        JPanel preview = new JPanel(new BorderLayout(0, 10));
        String count = tasks.size() == 1 ? "1 task" : tasks.size() + " tasks";
        preview.add(PlannerTheme.label("Add " + count + " to the selected plan", 18, true), BorderLayout.NORTH);
        preview.add(scroll, BorderLayout.CENTER);
        preview.add(PlannerTheme.label("Imported tasks start unfinished.", 13, false), BorderLayout.SOUTH);

        Object[] options = {"Import tasks", "Cancel"};
        int result = JOptionPane.showOptionDialog(parent, preview, "Review CSV import", JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        return result == JOptionPane.OK_OPTION ? tasks : List.of();
    }

    private static JTable previewTable(List<TaskDraft> tasks) {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Task", "Minutes", "Priority", "Due"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        for (TaskDraft draft : tasks) {
            model.addRow(new Object[]{draft.title(), draft.minutes(), draft.priority(), draft.dueDate()});
        }
        JTable table = new JTable(model);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.putClientProperty("html.disable", Boolean.TRUE);
        table.setDefaultRenderer(Object.class, renderer);
        table.setRowHeight(28);
        table.getColumnModel().getColumn(0).setPreferredWidth(325);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        return table;
    }
}
