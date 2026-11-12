package app.patterns.factory;

import app.model.*;
import java.util.List;

public final class CsvExportService extends ExportService {
    private static String q(String text) { return "\"" + text.replace("\"", "\"\"") + "\""; }
    @Override protected ReportFormatter createFormatter() {
        return new ReportFormatter() {
            @Override public String format(StudyPlan plan, List<StudyTask> tasks,
                                            List<FocusSession> sessions, String summary) {
                StringBuilder out = new StringBuilder("type,id,title_or_task,minutes,priority,date,status\n");
                out.append("plan,").append(q(plan.id())).append(',').append(q(plan.title())).append(",,,,")
                    .append(q(plan.goal())).append('\n');
                for (StudyTask task : tasks) {
                    out.append("task,").append(q(task.id())).append(',').append(q(task.title())).append(',')
                        .append(task.estimateMinutes()).append(',').append(task.priority()).append(',')
                        .append(task.dueDate()).append(',').append(task.completed()).append('\n');
                }
                for (FocusSession session : sessions) {
                    out.append("session,").append(q(session.id())).append(',').append(q(session.taskId())).append(',')
                        .append(session.minutes()).append(",,").append(session.date()).append(",\n");
                }
                out.append("summary,,").append(q(summary)).append(",,,,\n");
                return out.toString();
            }
        };
    }
}
