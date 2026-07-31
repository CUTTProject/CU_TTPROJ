package com.university.timetable_scheduler.solver;

/**
 * The search objective. Lower is better; {@code 0} means every hard constraint is satisfied.
 *
 * <p>The old scorer summed three counts with weight 1 each, so it could not tell a catastrophe
 * (an event nobody can attend because it was never placed) from a nuisance (two events clashing,
 * which one move might fix). Weighting them separates those.
 *
 * <p><b>Why room clashes and lecturer/student clashes share one counter:</b> both are hard
 * constraints of equal severity, so they carry the same weight — which means the search only ever
 * needs the <em>number of conflicting pairs</em>, not their reasons. A pair that clashes for both
 * reasons at once (same room <em>and</em> same lecturer) is one pair, counted once. That keeps the
 * hot path a single distinct-partner count instead of two categorised scans. The human-facing
 * breakdown is recovered separately and only once, at the end, by
 * {@link ConflictCounter#breakdown()}.
 */
public record SolutionCost(int unassignedEvents, int conflictingPairs) {

    /** An unplaced event is never acceptable — it must dominate any number of clashes. */
    public static final int WEIGHT_UNASSIGNED = 100;

    /** A hard-constraint violation between two placed events. */
    public static final int WEIGHT_CONFLICT_PAIR = 10;

    public int total() {
        return unassignedEvents * WEIGHT_UNASSIGNED + conflictingPairs * WEIGHT_CONFLICT_PAIR;
    }

    /** True when the assignment violates no hard constraint — the solver can stop immediately. */
    public boolean isFeasible() {
        return unassignedEvents == 0 && conflictingPairs == 0;
    }

    public boolean isBetterThan(SolutionCost other) {
        return other == null || total() < other.total();
    }

    @Override
    public String toString() {
        return "cost=" + total() + " (unassigned=" + unassignedEvents
                + ", conflictingPairs=" + conflictingPairs + ")";
    }
}
