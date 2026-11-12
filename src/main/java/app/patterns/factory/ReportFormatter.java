package app.patterns.factory;

import app.model.*;
import java.util.List;

public interface ReportFormatter {
    String format(StudyPlan plan, List<StudyTask> tasks, List<FocusSession> sessions, String summary);
}
