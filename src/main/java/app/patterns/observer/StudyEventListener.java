package app.patterns.observer;

@FunctionalInterface
public interface StudyEventListener {
    void onChange(StudyChangeEvent event);
}
