package app.patterns.adapter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface TaskImportSource {
    List<TaskDraft> read(Path path) throws IOException;
}
