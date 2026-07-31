package com.university.timetable_scheduler.solver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Random;

/**
 * MAX-MIN Ant System with a min-conflicts daemon — the hybrid that satisfies both halves of the
 * brief: real ACO with pheromones for the stakeholder, and the spec's Method 1 local search
 * (page 3) doing the exploitation.
 *
 * <p>One iteration:
 * <pre>
 *   for each ant:
 *       construct a full timetable, guided by τ^α · η^β     (AntSolutionBuilder — explore)
 *       polish it with min-conflicts                        (MinConflictsLocalSearch — exploit)
 *       keep it if it is the iteration's best
 *   update the global best                                  (elitism)
 *   evaporate τ, then let the global best deposit           (PheromoneMatrix — learn)
 *   if stagnating, flatten τ and start exploring again      (MMAS restart)
 * </pre>
 *
 * <p>Pure constructive ACO underperforms on university timetabling; hybridising with local search
 * is the standard remedy, and it is also what lets one implementation honour both documents.
 *
 * <p>The loop stops at the first of: cost 0 (provably feasible — nothing better exists), or
 * Time_Limit. It always returns the <b>best solution ever seen</b>.
 *
 * <p>That last point is a real bug fix, not a detail. The old loop took the best of five ants each
 * pass without including the incumbent in the comparison, so when every ant came out worse it
 * adopted the best ant anyway and the score was free to climb. A good solution found early could
 * be lost for good. Here {@code globalBest} is only ever replaced by something strictly better.
 *
 * <p>Single-threaded by design: deterministic under a fixed seed, which is worth more for
 * debugging and demos than the speedup would be.
 */
public final class AcoTimetableSolver {

    private static final Logger log = LoggerFactory.getLogger(AcoTimetableSolver.class);

    private final CspModel model;
    private final SolverParameters params;
    private final Random random;

    public AcoTimetableSolver(CspModel model, SolverParameters params, Random random) {
        this.model = model;
        this.params = params;
        this.random = random;
    }

    public SolverResult solve() {
        long startNanos = System.nanoTime();
        List<Integer> unschedulable = model.structurallyUnschedulableEvents();

        if (model.eventCount() == 0) {
            return new SolverResult(Solution.empty(0), new SolutionCost(0, 0),
                    new ConflictCounter.CostBreakdown(0, 0, 0, java.util.Set.of()),
                    0, Duration.ZERO, SolverResult.StopReason.NOTHING_TO_SOLVE, List.of());
        }

        if (!unschedulable.isEmpty()) {
            log.warn("{} event(s) have an empty domain and cannot be scheduled by any algorithm — "
                    + "no contiguous timeslot block matches their duration. Excluded from the search.",
                    unschedulable.size());
        }

        Deadline deadline = Deadline.in(Duration.ofSeconds(params.getTimeLimitSeconds()));
        PheromoneMatrix pheromones = new PheromoneMatrix(model, params);
        AntSolutionBuilder builder = new AntSolutionBuilder(model, pheromones, params, random);
        MinConflictsLocalSearch localSearch = new MinConflictsLocalSearch(model, params, random);

        Solution globalBest = null;
        SolutionCost globalBestCost = null;
        SolverResult.StopReason stopReason = SolverResult.StopReason.TIME_LIMIT_REACHED;

        int iteration = 0;
        int iterationsSinceImprovement = 0;

        logStart();

        while (!deadline.isExpired()) {
            iteration++;

            // ── Ants: construct, then polish ────────────────────────────────────────────
            Solution iterationBest = null;
            SolutionCost iterationBestCost = null;

            for (int ant = 0; ant < params.getAnts() && !deadline.isExpired(); ant++) {
                ConflictCounter counter = builder.build();
                SolutionCost cost = localSearch.improve(counter, deadline);

                if (cost.isBetterThan(iterationBestCost)) {
                    iterationBestCost = cost;
                    iterationBest = counter.solution().copy();
                }
            }

            // Deadline hit mid-iteration before any ant finished — nothing to learn from.
            if (iterationBest == null) break;

            // ── Elitism: the global best is only ever replaced by something strictly better ──
            if (iterationBestCost.isBetterThan(globalBestCost)) {
                globalBest = iterationBest;
                globalBestCost = iterationBestCost;
                iterationsSinceImprovement = 0;

                // τmax is defined in terms of the incumbent's cost, so retune whenever it moves.
                pheromones.recalculateBounds(globalBestCost.total());
                logImprovement(iteration, globalBestCost);
            } else {
                iterationsSinceImprovement++;
            }

            if (globalBestCost.isFeasible()) {
                stopReason = SolverResult.StopReason.FEASIBLE_SOLUTION_FOUND;
                break;
            }

            // ── Learn ───────────────────────────────────────────────────────────────────
            pheromones.evaporate();
            // MMAS: only the winner deposits. Using the global best rather than the iteration
            // best keeps reinforcement pointed at the best structure known, not merely the best
            // of this round — which may be considerably worse.
            pheromones.deposit(globalBest, globalBestCost, params.getDepositConstant());

            // ── Restart on stagnation ───────────────────────────────────────────────────
            if (iterationsSinceImprovement >= params.getStagnationLimit()) {
                log.info("Stagnated for {} iterations — reinitialising pheromone trails "
                        + "(global best is kept)", iterationsSinceImprovement);
                pheromones.reinitialise();
                iterationsSinceImprovement = 0;
            }

            logProgress(iteration, iterationBestCost, globalBestCost);
        }

        // Nothing usable at all (e.g. deadline of zero) — hand back an empty assignment.
        if (globalBest == null) {
            globalBest = Solution.empty(model.eventCount());
            globalBestCost = new ConflictCounter(model, globalBest).cost();
        }

        ConflictCounter finalCounter = new ConflictCounter(model, globalBest.copy());
        ConflictCounter.CostBreakdown breakdown = finalCounter.breakdown();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);

        logFinish(globalBestCost, breakdown, iteration, elapsed, stopReason);

        return new SolverResult(globalBest, globalBestCost, breakdown, iteration, elapsed,
                stopReason, toEventIds(unschedulable));
    }

    private List<java.util.UUID> toEventIds(List<Integer> eventIndices) {
        return eventIndices.stream().map(e -> model.event(e).getId()).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Logging — the run should be explainable after the fact, not a black box
    // ─────────────────────────────────────────────────────────────────────────────

    private void logStart() {
        if (!params.isVerbose()) return;
        log.info("ACO+min-conflicts starting: {} events, avg domain {}, {} ants, "
                        + "alpha={}, beta={}, rho={}, timeLimit={}s",
                model.eventCount(), String.format("%.1f", model.averageDomainSize()),
                params.getAnts(), params.getAlpha(), params.getBeta(),
                params.getEvaporationRate(), params.getTimeLimitSeconds());
    }

    private void logImprovement(int iteration, SolutionCost cost) {
        if (!params.isVerbose()) return;
        log.info("Iteration {} — new global best: {}", iteration, cost);
    }

    private void logProgress(int iteration, SolutionCost iterationBest, SolutionCost globalBest) {
        if (!params.isVerbose() || iteration % 25 != 0) return;
        log.info("Iteration {} — iterationBest={}, globalBest={}",
                iteration, iterationBest.total(), globalBest.total());
    }

    private void logFinish(SolutionCost cost, ConflictCounter.CostBreakdown breakdown,
                           int iterations, Duration elapsed, SolverResult.StopReason reason) {
        if (!params.isVerbose()) return;
        log.info("""
                        ACO finished ({}) after {} iterations in {}s
                          feasible:                  {}
                          unassigned:                {}
                          room clashes:              {}
                          lecturer/student clashes:  {}
                          total cost:                {}""",
                reason, iterations, elapsed.toSeconds(), cost.isFeasible(),
                breakdown.unassignedEvents(), breakdown.roomClashes(),
                breakdown.lecturerStudentClashes(), cost.total());
    }
}
