package app.patterns.decorator;

import app.model.StudyPlan;

public final class BaseSummary implements SummaryComponent {
    private final StudyPlan plan;
    public BaseSummary(StudyPlan plan) { this.plan = plan; }
    @Override public String render() {
        return "STUDY SPRINT REPORT\n\nPlan: " + plan.title() + "\nGoal: " + plan.goal() + "\n";
    }
}
