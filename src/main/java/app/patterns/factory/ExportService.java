package app.patterns.factory;

import app.model.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public abstract class ExportService {
    protected abstract ReportFormatter createFormatter();
    public final void export(StudyPlan plan, List<StudyTask> tasks, List<FocusSession> sessions,
                             String summary, Path path) throws IOException {
        String content = createFormatter().format(plan, tasks, sessions, summary);
        Path absolute = path.toAbsolutePath();
        if (absolute.getParent() != null) Files.createDirectories(absolute.getParent());
        Files.writeString(absolute, content, StandardCharsets.UTF_8);
    }
}
