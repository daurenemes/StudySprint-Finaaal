package app.storage;

import app.model.Snapshot;
import java.io.IOException;

public interface StudyRepository {
    Snapshot load() throws IOException;
    void save(Snapshot snapshot) throws IOException;
}
