package com.university.timetable_scheduler.solver;

/**
 * The pheromone table τ — the colony's shared memory, and the thing the previous implementation
 * was missing entirely (it had a constant named {@code ANTS} and nothing else).
 *
 * <p>{@code τ[e][k]} is the learned desirability of giving event {@code e} candidate {@code k}.
 * Every iteration, good placements are reinforced and everything slowly fades, so the colony
 * gradually concentrates on regions of the search space that have paid off — while
 * {@link AntSolutionBuilder} keeps sampling probabilistically so it never fully commits.
 *
 * <p>This is the <b>MAX-MIN Ant System</b> (Stützle &amp; Hoos) variant:
 * <ol>
 *   <li><b>Only the best ant deposits.</b> Letting every ant deposit averages the signal into
 *       mush; MMAS follows the winner.</li>
 *   <li><b>τ is clamped to {@code [τmin, τmax]}.</b> The safeguard that makes (1) survivable —
 *       without a floor, a candidate that goes unused early drops to ~0 and can never be sampled
 *       again, and the colony converges prematurely onto whatever it stumbled on first.</li>
 *   <li><b>τ starts at τmax.</b> An optimistic start means heavy early exploration, with
 *       evaporation gradually sharpening the distribution.</li>
 * </ol>
 */
public final class PheromoneMatrix {

    private final double[][] tau;
    private final double evaporationRate;
    private final double averageDomainSize;

    private double tauMax;
    private double tauMin;

    public PheromoneMatrix(CspModel model, SolverParameters params) {
        this.evaporationRate = params.getEvaporationRate();
        this.averageDomainSize = Math.max(1.0, model.averageDomainSize());

        this.tau = new double[model.eventCount()][];
        for (int e = 0; e < model.eventCount(); e++) {
            tau[e] = new double[model.domainOf(e).size()];
        }

        // No best cost known yet: seed the bounds from a deliberately optimistic cost of 1 so the
        // table starts flat and maximal. recalculateBounds() tightens them once ants report back.
        recalculateBounds(1);
        reinitialise();
    }

    public double get(int event, int candidate) {
        return tau[event][candidate];
    }

    /** Flattens τ back to τmax everywhere — a fresh start after stagnation. */
    public void reinitialise() {
        for (double[] row : tau) {
            java.util.Arrays.fill(row, tauMax);
        }
    }

    /**
     * Global evaporation: {@code τ ← (1 − ρ) · τ}, floored at τmin.
     *
     * <p>Run every iteration, before deposit. This is the colony forgetting — without it, early
     * accidents accumulate forever and later evidence can never outweigh them.
     */
    public void evaporate() {
        double retained = 1.0 - evaporationRate;
        for (double[] row : tau) {
            for (int k = 0; k < row.length; k++) {
                row[k] = Math.max(tauMin, row[k] * retained);
            }
        }
    }

    /**
     * Reinforces the placements that {@code best} actually used: {@code τ ← τ + Q / (1 + cost)},
     * capped at τmax. Cheaper solutions deposit more, so the trail follows quality.
     *
     * <p>Only ever called with a single winning solution — see the class note on MMAS.
     */
    public void deposit(Solution best, SolutionCost cost, double q) {
        double amount = q / (1.0 + cost.total());
        for (int e = 0; e < best.eventCount(); e++) {
            if (!best.isAssigned(e)) continue;
            int k = best.choiceOf(e);
            tau[e][k] = Math.min(tauMax, tau[e][k] + amount);
        }
    }

    /**
     * Retunes {@code [τmin, τmax]} to the best cost seen so far, per the MMAS formulae:
     * <pre>
     *   τmax = 1 / (ρ · (1 + cost_best))
     *   τmin = τmax / (2 · averageDomainSize)
     * </pre>
     * τmax tracks the incumbent because the useful range of τ depends on how good "good" currently
     * is. τmin scales with domain size: the more candidates competing for an event, the lower the
     * floor has to sit before it stops flattening the distribution into noise.
     */
    public void recalculateBounds(int bestCost) {
        this.tauMax = 1.0 / (evaporationRate * (1.0 + bestCost));
        this.tauMin = tauMax / (2.0 * averageDomainSize);
    }

    public double tauMax() { return tauMax; }
    public double tauMin() { return tauMin; }
}
