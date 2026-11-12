package app.patterns.command;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public final class CommandHistory {
    private final Deque<StudyCommand> undo = new ArrayDeque<>();
    private final Deque<StudyCommand> redo = new ArrayDeque<>();

    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean canRedo() { return !redo.isEmpty(); }

    public void execute(StudyCommand command) throws IOException {
        command.execute();
        undo.push(command);
        redo.clear();
        if (undo.size() > 50) undo.removeLast();
    }

    public void undo() throws IOException {
        if (!canUndo()) return;
        StudyCommand command = undo.peek();
        command.undo();
        redo.push(undo.pop());
    }

    public void redo() throws IOException {
        if (!canRedo()) return;
        StudyCommand command = redo.peek();
        command.execute();
        undo.push(redo.pop());
    }

    public void clear() {
        undo.clear();
        redo.clear();
    }
}
