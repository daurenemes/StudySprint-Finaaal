package app.patterns.decorator;

import app.model.FocusSession;
import java.util.List;

public final class FocusSummaryDecorator extends SummaryDecorator {
    private final List<FocusSession> sessions;
    public FocusSummaryDecorator(SummaryComponent base, List<FocusSession> sessions) {
        super(base);
        this.sessions = List.copyOf(sessions);
    }
    @Override public String render() {
        int total = sessions.stream().mapToInt(FocusSession::minutes).sum();
        return base.render() + "\nFOCUS SESSIONS\nSessions: " + sessions.size()
            + "\nActual focus time: " + total + " min\n";
    }
}
