package app.service;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.function.LongSupplier;

public final class FocusTimer {
    private final String id = UUID.randomUUID().toString();
    private final LongSupplier nanoTime;
    private final int targetMinutes;
    private TimerState state;

    public FocusTimer(int minutes, LongSupplier nanoTime) {
        if (minutes < 1 || minutes > 120) throw new IllegalArgumentException("Choose 1–120 minutes");
        this.nanoTime = Objects.requireNonNull(nanoTime);
        targetMinutes = minutes;
        state = new Idle(Duration.ofMinutes(minutes).toNanos());
    }

    public void start() { state = state.start(nanoTime.getAsLong()); }
    public void pause() { state = state.pause(nanoTime.getAsLong()); }
    public boolean running() { return update() instanceof Running; }
    public boolean ready() { return update() == Finished.INSTANCE; }
    String id() { return id; }
    public int targetMinutes() { return targetMinutes; }

    public long remainingSeconds() { return reading().remainingSeconds(); }

    public Reading reading() {
        long now = nanoTime.getAsLong();
        state = state.update(now);
        long seconds = (state.remaining(now) + 999_999_999L) / 1_000_000_000L;
        return new Reading(seconds, state instanceof Running);
    }

    public record Reading(long remainingSeconds, boolean running) { }

    private TimerState update() {
        state = state.update(nanoTime.getAsLong());
        return state;
    }

    private interface TimerState {
        long remaining(long now);
        default TimerState start(long now) { return this; }
        default TimerState pause(long now) { return this; }
        default TimerState update(long now) { return this; }
    }

    private record Idle(long duration) implements TimerState {
        @Override public long remaining(long now) { return duration; }
        @Override public TimerState start(long now) { return new Running(duration, now); }
    }

    private record Running(long duration, long startedAt) implements TimerState {
        @Override public long remaining(long now) {
            return Math.max(0, duration - (now - startedAt));
        }

        @Override public TimerState pause(long now) {
            long left = remaining(now);
            return left == 0 ? Finished.INSTANCE : new Paused(left);
        }

        @Override public TimerState update(long now) {
            return remaining(now) == 0 ? Finished.INSTANCE : this;
        }
    }

    private record Paused(long duration) implements TimerState {
        @Override public long remaining(long now) { return duration; }
        @Override public TimerState start(long now) { return new Running(duration, now); }
    }

    private enum Finished implements TimerState {
        INSTANCE;

        @Override public long remaining(long now) { return 0; }
    }
}
