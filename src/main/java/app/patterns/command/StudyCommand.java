package app.patterns.command;

import java.io.IOException;

public interface StudyCommand {
    void execute() throws IOException;
    void undo() throws IOException;
}
