package com.university.timetable_scheduler.solver;

/**
 * The pheromone table τ — the colony's shared memory. {@code τ[e][k]} is the learned desirability of
 * giving event {@code e} candidate {@code k}: good placements are reinforced, everything else fades.
 *
 * <p><b>MAX-MIN Ant System</b> (Stützle &amp; Hoos): only the best ant deposits, since averaging
 * every ant's deposit turns the signal to mush; τ is clamped to {@code [τmin, τmax]}, without which
 * an early-unused candidate drops to ~0 and can never be sampled again; and τ starts at τmax, so
 * early search explores broadly and evaporation sharpens it.
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
     * Retunes the band to the best cost so far, per the MMAS formulae:
     * {@code τmax = 1 / (ρ · (1 + cost_best))} and {@code τmin = τmax / (2 · averageDomainSize)}.
     *
     * <p><b>Existing values are clamped into the new band.</b> {@link #evaporate()} only floors and
     * {@link #deposit} only caps, so nothing else pulls a stranded cell back. On the first call every
     * cell sits above the new ceiling — deposit would snap the winner's cells down while the rejected
     * ones stayed high, inverting the signal for ~50 iterations.
     */
    public void recalculateBounds(int bestCost) {
        this.tauMax = 1.0 / (evaporationRate * (1.0 + bestCost));
        this.tauMin = tauMax / (2.0 * averageDomainSize);

        for (double[] row : tau) {
            for (int k = 0; k < row.length; k++) {
                row[k] = Math.min(tauMax, Math.max(tauMin, row[k]));
            }
        }
    }

    public double tauMax() { return tauMax; }
    public double tauMin() { return tauMin; }
}
