package com.university.timetable_scheduler.solver;

import com.university.timetable_scheduler.entity.Event;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the MAX-MIN Ant System pheromone rules — evaporation, deposit and the τ bounds.
 *
 * <p>The bounds are the load-bearing part. Because only the best ant deposits, an unbounded τ would
 * let unused candidates decay towards zero, drop out of the roulette wheel for good, and converge
 * the colony onto whatever it happened to find first. τmin is what keeps the search alive.
 */
class PheromoneMatrixTest {

    private CspModel smallModel() {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < 3; i++) events.add(SolverFixture.event(Duration.ofHours(1)));
        return SolverFixture.model(events, SolverFixture.rooms(2),
                SolverFixture.slots(1, 4), SolverFixture.noConflicts(3));
    }

    @Test
    @DisplayName("starts flat at tauMax, so early ants explore broadly")
    void initialisesToTauMax() {
        CspModel model = smallModel();
        PheromoneMatrix pheromones = new PheromoneMatrix(model, SolverFixture.params(1));

        for (int e = 0; e < model.eventCount(); e++) {
            for (int k = 0; k < model.domainOf(e).size(); k++) {
                assertThat(pheromones.get(e, k)).isEqualTo(pheromones.tauMax());
            }
        }
    }

    @Test
    @DisplayName("evaporation multiplies by (1 - rho) but never falls below tauMin")
    void evaporationDecaysAndFloorsAtTauMin() {
        CspModel model = smallModel();
        SolverParameters params = SolverFixture.params(1);
        params.setEvaporationRate(0.5);

        PheromoneMatrix pheromones = new PheromoneMatrix(model, params);
        double start = pheromones.get(0, 0);

        pheromones.evaporate();
        assertThat(pheromones.get(0, 0)).isEqualTo(start * 0.5);

        // Run it into the ground: the floor must hold, or this candidate can never be picked again.
        for (int i = 0; i < 200; i++) pheromones.evaporate();
        assertThat(pheromones.get(0, 0)).isEqualTo(pheromones.tauMin());
        assertThat(pheromones.tauMin()).isPositive();
    }

    @Test
    @DisplayName("only the candidates the best solution used are reinforced")
    void depositReinforcesOnlyTheUsedCandidates() {
        CspModel model = smallModel();
        SolverParameters params = SolverFixture.params(1);
        PheromoneMatrix pheromones = new PheromoneMatrix(model, params);

        Solution best = Solution.empty(model.eventCount());
        best.setChoice(0, 2);   // event 0 took candidate 2; nothing else is placed

        pheromones.evaporate();
        double untouched = pheromones.get(0, 3);
        pheromones.deposit(best, new SolutionCost(0, 1), params.getDepositConstant());

        assertThat(pheromones.get(0, 2)).isGreaterThan(untouched);
        assertThat(pheromones.get(0, 3)).isEqualTo(untouched);
    }

    @Test
    @DisplayName("deposit is capped at tauMax")
    void depositIsCappedAtTauMax() {
        CspModel model = smallModel();
        SolverParameters params = SolverFixture.params(1);
        PheromoneMatrix pheromones = new PheromoneMatrix(model, params);

        Solution best = Solution.empty(model.eventCount());
        best.setChoice(0, 1);

        for (int i = 0; i < 500; i++) {
            pheromones.deposit(best, new SolutionCost(0, 1), params.getDepositConstant());
        }
        assertThat(pheromones.get(0, 1)).isEqualTo(pheromones.tauMax());
    }

    @Test
    @DisplayName("a cheaper solution deposits more than an expensive one")
    void betterSolutionsDepositMore() {
        CspModel model = smallModel();
        SolverParameters params = SolverFixture.params(1);

        PheromoneMatrix cheap = new PheromoneMatrix(model, params);
        PheromoneMatrix expensive = new PheromoneMatrix(model, params);

        Solution best = Solution.empty(model.eventCount());
        best.setChoice(0, 0);

        cheap.evaporate();
        expensive.evaporate();
        cheap.deposit(best, new SolutionCost(0, 1), params.getDepositConstant());
        expensive.deposit(best, new SolutionCost(0, 50), params.getDepositConstant());

        assertThat(cheap.get(0, 0)).isGreaterThan(expensive.get(0, 0));
    }

    @Test
    @DisplayName("bounds tighten as the incumbent improves")
    void boundsTrackTheIncumbentCost() {
        CspModel model = smallModel();
        PheromoneMatrix pheromones = new PheromoneMatrix(model, SolverFixture.params(1));

        pheromones.recalculateBounds(100);
        double loose = pheromones.tauMax();

        pheromones.recalculateBounds(10);
        assertThat(pheromones.tauMax()).isGreaterThan(loose);
        assertThat(pheromones.tauMin()).isLessThan(pheromones.tauMax());
    }

    @Test
    @DisplayName("reinitialise flattens the table back to tauMax")
    void reinitialiseRestoresExploration() {
        CspModel model = smallModel();
        PheromoneMatrix pheromones = new PheromoneMatrix(model, SolverFixture.params(1));

        for (int i = 0; i < 50; i++) pheromones.evaporate();
        assertThat(pheromones.get(0, 0)).isLessThan(pheromones.tauMax());

        pheromones.reinitialise();
        assertThat(pheromones.get(0, 0)).isEqualTo(pheromones.tauMax());
    }
}
