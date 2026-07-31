package com.university.timetable_scheduler.solver;

import java.time.Duration;

/**
 * A wall-clock budget — the spec's {@code Time_Limit} input, which the flowchart checks on every
 * pass ("Time_Limit reached?").
 *
 * <p>The previous implementation had no equivalent: it counted 10,000 iterations instead, so its
 * runtime varied with instance size and could not be predicted or promised. A stakeholder can hold
 * you to "under ten minutes"; nobody can act on "ten thousand iterations".
 *
 * <p>Uses {@link System#nanoTime()} — monotonic, so an NTP correction mid-solve cannot make the
 * budget jump.
 */
public record Deadline(long expiryNanoTime) {

    public static Deadline in(Duration budget) {
        return new Deadline(System.nanoTime() + budget.toNanos());
    }

    public boolean isExpired() {
        return System.nanoTime() >= expiryNanoTime;
    }

    public Duration remaining() {
        long remaining = expiryNanoTime - System.nanoTime();
        return remaining <= 0 ? Duration.ZERO : Duration.ofNanos(remaining);
    }

    public Duration elapsedSince(long startNanoTime) {
        return Duration.ofNanos(System.nanoTime() - startNanoTime);
    }
}
