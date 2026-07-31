package com.university.timetable_scheduler.solver;

import com.university.timetable_scheduler.entity.Event;
import com.university.timetable_scheduler.entity.Room;
import com.university.timetable_scheduler.entity.Timeslot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the ACO + min-conflicts hybrid, on instances whose answers are known.
 */
class AcoTimetableSolverTest {

    private static final Duration ONE_HOUR = Duration.ofHours(1);
    private static final Duration TWO_HOURS = Duration.ofHours(2);

    private static SolverResult solve(CspModel model, SolverParameters params, long seed) {
        return new AcoTimetableSolver(model, params, new Random(seed)).solve();
    }

    private static List<Event> events(int count, Duration duration) {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < count; i++) events.add(SolverFixture.event(duration));
        return events;
    }

    @Test
    @DisplayName("solves a tight instance whose only answers are permutations")
    void solvesTightCliqueInstance() {
        // 5 mutually-conflicting events, 1 room, exactly 5 slots. Every event must take a distinct
        // slot, so the only feasible solutions are the 5! permutations — no slack at all.
        List<Event> events = events(5, ONE_HOUR);
        CspModel model = SolverFixture.model(events, SolverFixture.rooms(1),
                SolverFixture.slots(1, 5), SolverFixture.clique(5));

        SolverResult result = solve(model, SolverFixture.params(20), 1L);

        assertThat(result.isFeasible()).isTrue();
        assertThat(result.cost().total()).isZero();
        assertThat(result.stoppedBecause()).isEqualTo(SolverResult.StopReason.FEASIBLE_SOLUTION_FOUND);
    }

    @Test
    @DisplayName("solves a realistically sized instance well inside the budget")
    void solvesLargerInstance() {
        // 60 events over 4 rooms × 40 slots, with a conflict chain running through them.
        int count = 60;
        List<Event> events = events(count, ONE_HOUR);

        int[][] adjacency = new int[count][];
        for (int i = 0; i < count; i++) {
            List<Integer> neighbours = new ArrayList<>();
            if (i > 0) neighbours.add(i - 1);
            if (i < count - 1) neighbours.add(i + 1);
            if (i + 7 < count) neighbours.add(i + 7);
            if (i - 7 >= 0) neighbours.add(i - 7);
            adjacency[i] = neighbours.stream().mapToInt(Integer::intValue).toArray();
        }

        CspModel model = SolverFixture.model(events, SolverFixture.rooms(4),
                SolverFixture.slots(5, 8), adjacency);

        SolverResult result = solve(model, SolverFixture.params(30), 7L);

        assertThat(result.isFeasible()).isTrue();
        assertThat(result.elapsed()).isLessThan(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("multi-hour events are placed as contiguous blocks that do not overlap")
    void handlesMultiHourEventsWithoutOverlap() {
        // 4 two-hour events, all mutually conflicting, 1 room, 8 slots on one day → feasible only
        // if each takes a distinct 2-hour block. This is the case a "ti != tj" reading of the spec
        // would get wrong: two blocks can start at different slots and still collide.
        List<Event> events = events(4, TWO_HOURS);
        CspModel model = SolverFixture.model(events, SolverFixture.rooms(1),
                SolverFixture.slots(1, 8), SolverFixture.clique(4));

        SolverResult result = solve(model, SolverFixture.params(20), 3L);

        assertThat(result.isFeasible()).isTrue();
        assertNoOverlappingBlocks(model, result);
    }

    /** Independently re-checks the answer, rather than trusting the solver's own cost. */
    private void assertNoOverlappingBlocks(CspModel model, SolverResult result) {
        Solution solution = result.bestSolution();
        for (int a = 0; a < model.eventCount(); a++) {
            for (int b = a + 1; b < model.eventCount(); b++) {
                if (!solution.isAssigned(a) || !solution.isAssigned(b)) continue;

                Candidate ca = model.candidate(a, solution.choiceOf(a));
                Candidate cb = model.candidate(b, solution.choiceOf(b));
                if (!ca.block().overlapsWith(cb.block())) continue;

                assertThat(ca.roomIndex())
                        .as("events %s and %s overlap in time and must not share a room", a, b)
                        .isNotEqualTo(cb.roomIndex());
            }
        }
    }

    @Test
    @DisplayName("a fixed seed reproduces the run exactly")
    void isReproducibleWithAFixedSeed() {
        // Reproducibility is why SolverParameters has a seed: the old solver used an unseeded
        // Random, so a disputed timetable could never be reproduced and investigated.
        List<Event> events = events(20, ONE_HOUR);
        CspModel model = SolverFixture.model(events, SolverFixture.rooms(2),
                SolverFixture.slots(2, 6), SolverFixture.clique(20));

        SolverParameters params = SolverFixture.params(5);
        SolverResult first = solve(model, params, 42L);
        SolverResult second = solve(model, params, 42L);

        assertThat(second.bestSolution().rawChoices())
                .containsExactly(first.bestSolution().rawChoices());
        assertThat(second.cost().total()).isEqualTo(first.cost().total());
    }

    @Test
    @DisplayName("reports events that no algorithm could ever place, instead of grinding on them")
    void reportsStructurallyUnschedulableEvents() {
        // A 90-minute event against 1-hour slots matches no contiguous block, so its domain is
        // empty. The old loop spent all 10,000 iterations failing to fix this, silently. It must
        // now be excluded from the search, reported, and must not stop the rest from solving.
        List<Event> events = new ArrayList<>(events(3, ONE_HOUR));
        Event impossible = SolverFixture.event(Duration.ofMinutes(90));
        events.add(impossible);

        CspModel model = SolverFixture.model(events, SolverFixture.rooms(1),
                SolverFixture.slots(1, 6), SolverFixture.noConflicts(4));

        SolverResult result = solve(model, SolverFixture.params(10), 5L);

        assertThat(result.unschedulableEventIds()).containsExactly(impossible.getId());
        assertThat(result.isFeasible()).isTrue();  // the other three still solve
        assertThat(result.stoppedBecause()).isEqualTo(SolverResult.StopReason.FEASIBLE_SOLUTION_FOUND);
    }

    @Test
    @DisplayName("returns the best solution found on an unsatisfiable instance rather than failing")
    void returnsBestEffortWhenInfeasible() {
        // 6 mutually-conflicting events but only 3 slots in 1 room: provably unsatisfiable
        // (pigeonhole). The solver must burn its budget, then hand back its best attempt with the
        // clashes visible — a partial timetable is more useful to a scheduler than an exception.
        List<Event> events = events(6, ONE_HOUR);
        CspModel model = SolverFixture.model(events, SolverFixture.rooms(1),
                SolverFixture.slots(1, 3), SolverFixture.clique(6));

        SolverResult result = solve(model, SolverFixture.params(3), 11L);

        assertThat(result.isFeasible()).isFalse();
        assertThat(result.stoppedBecause()).isEqualTo(SolverResult.StopReason.TIME_LIMIT_REACHED);
        assertThat(result.bestSolution()).isNotNull();
        assertThat(result.breakdown().roomClashes()).isPositive();
        // Every event still gets placed; they simply cannot all be placed cleanly.
        assertThat(result.cost().unassignedEvents()).isZero();
    }

    @Test
    @DisplayName("honours the time limit")
    void respectsTheTimeLimit() {
        // The spec's input is Time_Limit; the old code counted iterations instead, so its runtime
        // was unpredictable. On an unsatisfiable instance the solver runs to the limit — and must
        // then actually stop.
        List<Event> events = events(80, ONE_HOUR);
        CspModel model = SolverFixture.model(events, SolverFixture.rooms(1),
                SolverFixture.slots(1, 4), SolverFixture.clique(80));

        SolverParameters params = SolverFixture.params(2);
        SolverResult result = solve(model, params, 13L);

        assertThat(result.stoppedBecause()).isEqualTo(SolverResult.StopReason.TIME_LIMIT_REACHED);
        assertThat(result.elapsed()).isLessThan(Duration.ofSeconds(20));
    }

    @Test
    @DisplayName("beats a random assignment by a wide margin")
    void beatsRandomBaseline() {
        // The headline claim: this is a real optimiser, not a shuffle. Compare against sampling
        // uniformly from each domain — which is essentially what the old inner loop did.
        //
        // 40 mutually-conflicting events need 40 distinct timeslots, so the instance must offer at
        // least that many to be satisfiable at all — extra rooms cannot help, since conflicting
        // events may not share a time whatever room they are in. 50 slots leaves a little slack.
        List<Event> events = events(40, ONE_HOUR);
        CspModel model = SolverFixture.model(events, SolverFixture.rooms(2),
                SolverFixture.slots(5, 10), SolverFixture.clique(40));

        SolverResult result = solve(model, SolverFixture.params(15), 17L);

        Random random = new Random(17L);
        Solution baseline = Solution.empty(model.eventCount());
        ConflictCounter baselineCounter = new ConflictCounter(model, baseline);
        for (int e = 0; e < model.eventCount(); e++) {
            baselineCounter.assign(e, random.nextInt(model.domainOf(e).size()));
        }

        assertThat(result.cost().total()).isLessThan(baselineCounter.cost().total());
        assertThat(result.isFeasible()).isTrue();
    }

    @Test
    @DisplayName("an empty instance is handled without blowing up")
    void handlesEmptyInstance() {
        CspModel model = SolverFixture.model(List.<Event>of(), List.<Room>of(),
                List.<Timeslot>of(), new int[0][]);

        SolverResult result = solve(model, SolverFixture.params(5), 1L);

        assertThat(result.stoppedBecause()).isEqualTo(SolverResult.StopReason.NOTHING_TO_SOLVE);
        assertThat(result.cost().isFeasible()).isTrue();
    }
}
