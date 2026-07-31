package com.university.timetable_scheduler.solver;

import java.util.Random;

/**
 * <b>This class is Method 1 from the spec</b> — the page-3 flowchart, implemented as written.
 *
 * <p>Flowchart → code:
 * <pre>
 *   Input: CSP, Time_Limit          → the model, plus {@link Deadline}
 *   Current :- complete assignment  → the ant's constructed solution (already complete)
 *   Time_Limit reached?             → deadline.isExpired()
 *   Is Current the CSP solution?    → !counter.hasConflicts()   → return current
 *   Var :- a randomly chosen var    → counter.randomConflictedEvent(random)
 *   Value :- a value v (Note 1)     → argMinConflicts(...)      ← the piece that was missing
 *   Set Var = Value                 → counter.assign(var, value)
 *   Return current OR failure       → best-ever solution, feasible or not
 * </pre>
 *
 * <p>Note 1 is the important line: <i>"the value v for var that <b>minimises</b>
 * CONFLICTS(csp, var, v, current)"</i>. The old code partitioned candidates into clean/not-clean
 * and picked a random one — a boolean where the spec asks for an argmin. When nothing clean
 * existed it therefore chose a random <em>violating</em> slot rather than the least-bad one, which
 * is exactly the situation min-conflicts is for.
 *
 * <p>In the hybrid this runs as ACO's <b>daemon step</b>: each ant constructs a solution, then this
 * polishes it before it is scored and allowed to deposit pheromone. Construction explores; this
 * exploits. Most of the measurable improvement comes from here — which is why a pure constructive
 * ACO would underperform on this problem.
 *
 * <p>Two additions beyond the literal flowchart, both standard and both necessary:
 * <ul>
 *   <li><b>Random walk moves.</b> Pure min-conflicts stalls on plateaux where no single move
 *       improves anything. A small chance of an arbitrary move walks off them.</li>
 *   <li><b>Best-ever tracking.</b> Because walk moves (and ties) can make things worse, the search
 *       remembers its best state and restores it at the end. The flowchart's "return current" is
 *       safe only for a search that never worsens; this one can.</li>
 * </ul>
 */
public final class MinConflictsLocalSearch {

    private final CspModel model;
    private final SolverParameters params;
    private final Random random;

    public MinConflictsLocalSearch(CspModel model, SolverParameters params, Random random) {
        this.model = model;
        this.params = params;
        this.random = random;
    }

    /**
     * Improves the counter's solution in place, leaving it at the best state found.
     *
     * @return the cost of that best state
     */
    public SolutionCost improve(ConflictCounter counter, Deadline deadline) {
        Solution current = counter.solution();

        SolutionCost bestCost = counter.cost();
        Solution bestSolution = current.copy();

        for (int step = 0; step < params.getLocalSearchMaxSteps(); step++) {

            // "Is Current the CSP solution?" — no conflicts left, nothing to repair.
            if (!counter.hasConflicts()) break;

            // "Time_Limit reached?" — checked every 64 steps; nanoTime is cheap but not free,
            // and 64 steps of min-conflicts take microseconds.
            if ((step & 0x3F) == 0 && deadline.isExpired()) break;

            // "Var :- A randomly chosen variable" (restricted to conflicted ones — repairing a
            // satisfied variable cannot reduce the cost).
            int var = counter.randomConflictedEvent(random);
            if (model.domainOf(var).isEmpty()) continue;

            // "Value :- A value v for var (see Note 1)"
            int value = random.nextDouble() < params.getLocalSearchWalkProbability()
                    ? randomValue(var)
                    : argMinConflicts(var, counter);

            // "Set Var; {Var = Value}"
            counter.assign(var, value);

            SolutionCost cost = counter.cost();
            if (cost.isBetterThan(bestCost)) {
                bestCost = cost;
                bestSolution = current.copy();
                if (bestCost.isFeasible()) break;  // cannot do better than zero
            }
        }

        // Restore the best state seen, since walk moves may have wandered away from it.
        if (!sameChoices(current, bestSolution)) {
            restore(counter, bestSolution);
        }
        current.setCost(bestCost);
        return bestCost;
    }

    /**
     * Note 1: the value minimising {@code CONFLICTS(csp, var, v, current)}.
     *
     * <p>Ties are broken at random rather than by first-found. Deterministic tie-breaking makes the
     * search revisit the same states and stall — with 1-hour slots and interchangeable rooms, ties
     * here are the common case, not the exception.
     *
     * <p>Large domains are subsampled per {@link SolverParameters#getCandidateSampleSize()}, making
     * this a near-argmin rather than an exact one; set that to 0 for the literal spec behaviour.
     */
    private int argMinConflicts(int var, ConflictCounter counter) {
        int domainSize = model.domainOf(var).size();
        int limit = params.getCandidateSampleSize();
        boolean subsample = limit > 0 && domainSize > limit;
        int examined = subsample ? limit : domainSize;

        int bestValue = -1;
        int bestConflicts = Integer.MAX_VALUE;
        int tiesSeen = 0;

        for (int i = 0; i < examined; i++) {
            int k = subsample ? random.nextInt(domainSize) : i;
            int conflicts = counter.conflictsIfAssigned(var, k);

            if (conflicts < bestConflicts) {
                bestConflicts = conflicts;
                bestValue = k;
                tiesSeen = 1;
                if (conflicts == 0) break;  // cannot beat a clean placement
            } else if (conflicts == bestConflicts) {
                // Reservoir sampling: each tied candidate gets an equal chance without
                // needing to collect them into a list first.
                tiesSeen++;
                if (random.nextInt(tiesSeen) == 0) bestValue = k;
            }
        }
        return bestValue >= 0 ? bestValue : random.nextInt(domainSize);
    }

    private int randomValue(int var) {
        return random.nextInt(model.domainOf(var).size());
    }

    private static boolean sameChoices(Solution a, Solution b) {
        return java.util.Arrays.equals(a.rawChoices(), b.rawChoices());
    }

    /** Replays {@code target}'s choices through the counter so its indices stay consistent. */
    private void restore(ConflictCounter counter, Solution target) {
        for (int e = 0; e < target.eventCount(); e++) {
            int desired = target.choiceOf(e);
            if (counter.solution().choiceOf(e) == desired) continue;

            if (desired == Solution.UNASSIGNED) counter.unassign(e);
            else counter.assign(e, desired);
        }
    }
}
