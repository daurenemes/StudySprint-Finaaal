package app.patterns.observer;

import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;

public final class StudyEventHub {
    private final CopyOnWriteArrayList<StudyEventListener> listeners = new CopyOnWriteArrayList<>();
    public void subscribe(StudyEventListener listener) { listeners.addIfAbsent(listener); }
    public void unsubscribe(StudyEventListener listener) { listeners.remove(listener); }
    public void publish(String message) {
        StudyChangeEvent event = new StudyChangeEvent(message, LocalDateTime.now());
        for (StudyEventListener listener : listeners) listener.onChange(event);
    }
}
