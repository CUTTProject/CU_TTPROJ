package com.university.timetable_scheduler.solver;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Every knob the solver has, bound from {@code application.properties} under
 * {@code timetable.solver.*}. Defaults here are sane for a 10-minute budget; nothing needs to be
 * set for the solver to run.
 *
 * <p>These live in config rather than as constants because ACO is sensitive to α/β/ρ and the right
 * values depend on the instance. Retuning should not need a rebuild.
 */
@Component
@ConfigurationProperties(prefix = "timetable.solver")
@Getter
@Setter
public class SolverParameters {

    /**
     * Ants per iteration. Each independently constructs a full timetable, then improves it with
     * local search. More ants means better coverage per iteration but fewer iterations inside the
     * time budget — and iterations are what actually accumulate pheromone knowledge.
     */
    private int ants = 10;

    /**
     * α — pheromone weight in the transition rule. How much an ant trusts what the colony learned.
     * Raising it converges faster and risks locking in early.
     */
    private double alpha = 1.0;

    /**
     * β — heuristic weight. How much an ant trusts the immediate conflict count in front of it.
     * Held above α on purpose: for timetabling, "don't clash right now" is a strong signal, and
     * pheromone should refine that judgement rather than override it.
     */
    private double beta = 3.0;

    /**
     * ρ — evaporation rate per iteration. Low keeps memory long and exploration broad; high makes
     * the colony forget quickly and chase the incumbent.
     */
    private double evaporationRate = 0.05;

    /** Q — deposit scaling constant. Deposit is {@code Q / (1 + cost)}. */
    private double depositConstant = 1.0;

    /**
     * q0 — probability an ant takes the single best-looking candidate outright instead of sampling
     * (the ACS pseudo-random-proportional rule). 0 disables it: pure probabilistic sampling.
     */
    private double greedySelectionProbability = 0.1;

    /**
     * Candidates sampled per event during construction; 0 means consider the whole domain.
     *
     * <p>Domains are {@code blocks × rooms} and run to the thousands. Scoring all of them for every
     * event, ant and iteration is where the time budget goes to die. Evaluating a random subset is
     * standard ACO practice and costs little quality — pheromone still spans the full domain, this
     * only bounds how much of it any one ant looks at.
     */
    private int candidateSampleSize = 64;

    /** Wall-clock budget — the spec's {@code Time_Limit} input. */
    private long timeLimitSeconds = 600;

    /**
     * Min-conflicts steps applied to each ant's constructed solution (the daemon step).
     * This is where most of the actual improvement happens.
     */
    private int localSearchMaxSteps = 20_000;

    /**
     * Probability that min-conflicts takes a random move instead of the best one (WalkSAT-style).
     * Pure min-conflicts stalls on plateaux where no single move improves anything; the occasional
     * random kick is what walks it off them.
     */
    private double localSearchWalkProbability = 0.1;

    /**
     * Iterations without global-best improvement before τ is reset to τmax (the MMAS restart).
     * Signals the colony has converged on something it cannot improve; flattening τ reopens the
     * search without discarding the recorded global best.
     */
    private int stagnationLimit = 40;

    /**
     * RNG seed. Set it to make a run byte-for-byte reproducible — necessary for debugging,
     * benchmarking and demos. Leave null for a fresh random seed each run.
     */
    private Long seed = null;

    /** Whether to log per-iteration progress. */
    private boolean verbose = true;
}
