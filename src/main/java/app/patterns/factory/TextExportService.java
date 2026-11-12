package app.patterns.factory;

import app.model.*;
import java.util.List;

public final class TextExportService extends ExportService {
    @Override protected ReportFormatter createFormatter() {
        return new ReportFormatter() {
            @Override public String format(StudyPlan plan, List<StudyTask> tasks,
                                            List<FocusSession> sessions, String summary) {
                return summary;
            }
        };
    }
}
