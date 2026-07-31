package com.university.timetable_scheduler.solver;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * What the solver produces: the best timetable it found, plus enough context to explain it.
 *
 * @param bestSolution          best assignment found across the whole run, never the last one tried
 * @param cost                  {@code bestSolution}'s cost; {@code total() == 0} means feasible
 * @param breakdown             violations split by reason, for logs and the API response
 * @param iterations            colony iterations completed
 * @param elapsed               wall-clock time actually spent
 * @param stoppedBecause        why the loop ended — feasibility, time, or stagnation
 * @param unschedulableEventIds events with an empty domain: no timeslot block fits their duration,
 *                              so no algorithm could place them. Reported rather than silently
 *                              retried; they usually mean a data problem, e.g. a 90-minute event
 *                              against 1-hour slots.
 */
public record SolverResult(Solution bestSolution,
                           SolutionCost cost,
                           ConflictCounter.CostBreakdown breakdown,
                           int iterations,
                           Duration elapsed,
                           StopReason stoppedBecause,
                           List<UUID> unschedulableEventIds) {

    public enum StopReason {
        /** Cost hit zero — every hard constraint satisfied. */
        FEASIBLE_SOLUTION_FOUND,
        /** Time_Limit reached; returning the best found so far. */
        TIME_LIMIT_REACHED,
        /** Nothing to solve (no events, rooms, or timeslots). */
        NOTHING_TO_SOLVE
    }

    public boolean isFeasible() {
        return cost != null && cost.isFeasible();
    }
}
