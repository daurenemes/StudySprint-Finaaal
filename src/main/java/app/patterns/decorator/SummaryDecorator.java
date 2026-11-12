package app.patterns.decorator;

public abstract class SummaryDecorator implements SummaryComponent {
    protected final SummaryComponent base;
    protected SummaryDecorator(SummaryComponent base) { this.base = base; }
}
