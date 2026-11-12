package app.storage;

import app.model.*;
import java.time.LocalDate;
import java.util.List;

public final class SampleData {
    private SampleData() { }

    public static Snapshot create() {
        LocalDate day = LocalDate.now();
        StudyPlan a = new StudyPlan("demo-plan-java", "Java Patterns", "Prepare for the SDP final defense");
        StudyPlan b = new StudyPlan("demo-plan-math", "Discrete Mathematics", "Practice modular arithmetic and proofs");
        StudyTask t1 = StudyTask.builder("demo-task-1", a.id()).title("Review Strategy and Observer").estimateMinutes(40).priority(5).dueDate(day.plusDays(1)).build();
        StudyTask t2 = StudyTask.builder("demo-task-2", a.id()).title("Draw class and sequence diagrams").estimateMinutes(65).priority(4).dueDate(day.plusDays(2)).build();
        StudyTask t3 = StudyTask.builder("demo-task-3", b.id()).title("Solve congruence exercises").estimateMinutes(25).priority(3).dueDate(day.plusDays(1)).completed(true).build();
        return new Snapshot(List.of(a,b), List.of(t1,t2,t3), List.of(new FocusSession("demo-session-1", t3.id(), 25, day)));
    }
}
