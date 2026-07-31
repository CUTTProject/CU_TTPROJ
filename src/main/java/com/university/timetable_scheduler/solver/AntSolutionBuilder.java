package com.university.timetable_scheduler.solver;

import java.util.List;
import java.util.Random;

/**
 * One ant walking one timetable into existence.
 *
 * <p>The ant visits events in most-constrained-first order and picks each one's slot/room
 * probabilistically, weighing what the colony has learned (τ) against what looks good right now (η):
 *
 * <pre>
 *                τ[e][k]^α · η(e,k)^β
 *   P(e,k) = ─────────────────────────────
 *              Σ_j  τ[e][j]^α · η(e,j)^β
 *
 *   where η(e,k) = 1 / (1 + CONFLICTS(csp, e, k, current))
 * </pre>
 *
 * <p>Contrast with the old loop, which sampled <em>uniformly</em> from whichever candidates were
 * completely clean. Uniform sampling has no gradient and no memory: it could not prefer a
 * one-clash placement over a five-clash placement, and it could not remember that a given room
 * worked well last iteration. Both halves of the formula above exist to fix one of those.
 *
 * <p>Ants build from empty rather than perturbing an incumbent. That is what makes the pheromone
 * table meaningful — τ is only informative if it gets to shape a whole solution.
 */
public final class AntSolutionBuilder {

    private final CspModel model;
    private final PheromoneMatrix pheromones;
    private final SolverParameters params;
    private final Random random;

    /** Scratch buffers, reused across events so construction allocates nothing in the hot path. */
    private final int[] sampledCandidates;
    private final double[] weights;

    public AntSolutionBuilder(CspModel model, PheromoneMatrix pheromones,
                              SolverParameters params, Random random) {
        this.model = model;
        this.pheromones = pheromones;
        this.params = params;
        this.random = random;

        int maxDomain = 0;
        for (int e = 0; e < model.eventCount(); e++) {
            maxDomain = Math.max(maxDomain, model.domainOf(e).size());
        }
        int buffer = params.getCandidateSampleSize() > 0
                ? Math.min(maxDomain, params.getCandidateSampleSize())
                : maxDomain;
        this.sampledCandidates = new int[Math.max(1, buffer)];
        this.weights = new double[Math.max(1, buffer)];
    }

    /**
     * Constructs a complete timetable.
     *
     * <p>Returns the {@link ConflictCounter} rather than the bare {@link Solution} so the caller
     * can hand it straight to local search — the occupancy index is already warm, and rebuilding it
     * would repeat work the construction just did. The solution is reachable via
     * {@link ConflictCounter#solution()}.
     */
    public ConflictCounter build() {
        Solution solution = Solution.empty(model.eventCount());
        ConflictCounter counter = new ConflictCounter(model, solution);

        // Most-constrained-first: place the events with the fewest options while the timetable is
        // still empty enough to have room for them.
        for (int event : model.searchOrder()) {
            List<Candidate> domain = model.domainOf(event);
            if (domain.isEmpty()) continue;  // structurally unschedulable; reported, not retried

            int chosen = chooseCandidate(event, counter);
            counter.assign(event, chosen);
        }

        solution.setCost(counter.cost());
        return counter;
    }

    /** Applies the transition rule above to one event, returning a domain index. */
    private int chooseCandidate(int event, ConflictCounter counter) {
        int sampleCount = sampleCandidates(event);

        double totalWeight = 0.0;
        int bestIndex = 0;
        double bestWeight = -1.0;

        for (int i = 0; i < sampleCount; i++) {
            int k = sampledCandidates[i];

            // η — the greedy signal. This is CONFLICTS(...) from the spec's Note 1, the same
            // function min-conflicts uses. A clean placement scores 1.0; each extra clash
            // shrinks it hyperbolically.
            double eta = 1.0 / (1.0 + counter.conflictsIfAssigned(event, k));

            double weight = Math.pow(pheromones.get(event, k), params.getAlpha())
                          * Math.pow(eta, params.getBeta());

            weights[i] = weight;
            totalWeight += weight;
            if (weight > bestWeight) {
                bestWeight = weight;
                bestIndex = k;
            }
        }

        // ACS pseudo-random-proportional rule: occasionally skip the dice and exploit outright.
        if (params.getGreedySelectionProbability() > 0
                && random.nextDouble() < params.getGreedySelectionProbability()) {
            return bestIndex;
        }

        // Degenerate guard: if every weight underflowed to 0, fall back to the greedy pick rather
        // than letting the roulette wheel walk off the end of the array.
        if (totalWeight <= 0.0) return bestIndex;

        // Roulette wheel over the sampled candidates.
        double threshold = random.nextDouble() * totalWeight;
        double cumulative = 0.0;
        for (int i = 0; i < sampleCount; i++) {
            cumulative += weights[i];
            if (cumulative >= threshold) return sampledCandidates[i];
        }
        return sampledCandidates[sampleCount - 1];  // floating-point slack
    }

    /**
     * Loads {@link #sampledCandidates} with the domain indices this ant will actually weigh,
     * returning how many. Small domains are considered whole; large ones are subsampled (see
     * {@link SolverParameters#getCandidateSampleSize()}).
     */
    private int sampleCandidates(int event) {
        int domainSize = model.domainOf(event).size();
        int limit = params.getCandidateSampleSize();

        if (limit <= 0 || domainSize <= limit) {
            for (int k = 0; k < domainSize; k++) sampledCandidates[k] = k;
            return domainSize;
        }

        // Random draw with replacement. A repeated index only costs a duplicated evaluation, so
        // it is not worth the bookkeeping of drawing without replacement in the hot path.
        for (int i = 0; i < limit; i++) {
            sampledCandidates[i] = random.nextInt(domainSize);
        }
        return limit;
    }
}
