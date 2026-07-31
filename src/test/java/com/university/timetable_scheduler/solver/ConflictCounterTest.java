package com.university.timetable_scheduler.solver;

import com.university.timetable_scheduler.entity.Event;
import com.university.timetable_scheduler.entity.Room;
import com.university.timetable_scheduler.entity.Timeslot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the spec's {@code CONFLICTS(csp, var, v, current)}.
 *
 * <p>Everything else in the solver — the ants' heuristic η, min-conflicts' argmin, the cost — is
 * derived from this one function. If it miscounts, the whole search optimises the wrong thing while
 * still looking like it works, so these assert the rules directly rather than through the solver.
 */
class ConflictCounterTest {

    private static final Duration ONE_HOUR = Duration.ofHours(1);

    /** Two rooms, one day of 4 slots. Candidates are ordered block-major, room-minor. */
    private record Setup(CspModel model, ConflictCounter counter) {}

    private Setup setup(int eventCount, int[][] adjacency) {
        List<Timeslot> slots = SolverFixture.slots(1, 4);
        List<Room> rooms = SolverFixture.rooms(2);
        List<Event> events = new java.util.ArrayList<>();
        for (int i = 0; i < eventCount; i++) events.add(SolverFixture.event(ONE_HOUR));

        CspModel model = SolverFixture.model(events, rooms, slots, adjacency);
        return new Setup(model, new ConflictCounter(model, Solution.empty(eventCount)));
    }

    /** Domain index for "hour h, room r" given 2 rooms and 1-hour events. */
    private static int at(int hour, int room) {
        return hour * 2 + room;
    }

    @Test
    @DisplayName("no conflicts when two events sit in different rooms at the same time")
    void differentRoomsSameTimeIsFine() {
        Setup s = setup(2, SolverFixture.noConflicts(2));
        s.counter().assign(0, at(0, 0));

        // Same hour, other room, and not conflict-graph neighbours → legal.
        assertThat(s.counter().conflictsIfAssigned(1, at(0, 1))).isZero();
    }

    @Test
    @DisplayName("room exclusivity: same room at the same time is a conflict even without an edge")
    void sameRoomSameTimeClashes() {
        Setup s = setup(2, SolverFixture.noConflicts(2));   // deliberately no conflict-graph edge
        s.counter().assign(0, at(0, 0));

        assertThat(s.counter().conflictsIfAssigned(1, at(0, 0))).isEqualTo(1);
    }

    @Test
    @DisplayName("colouring: conflict-graph neighbours may not overlap, even in different rooms")
    void neighboursOverlappingInTimeClash() {
        Setup s = setup(2, SolverFixture.clique(2));
        s.counter().assign(0, at(0, 0));

        assertThat(s.counter().conflictsIfAssigned(1, at(0, 1))).isEqualTo(1);  // other room, still clashes
        assertThat(s.counter().conflictsIfAssigned(1, at(1, 1))).isZero();      // different hour → fine
    }

    @Test
    @DisplayName("a pair breaking both rules at once counts once, not twice")
    void bothRulesAtOnceCountsOnce() {
        Setup s = setup(2, SolverFixture.clique(2));
        s.counter().assign(0, at(0, 0));

        // Same room AND a conflict-graph neighbour. SolutionCost assumes distinct pairs, so this
        // must be 1 — double-counting would silently inflate the objective.
        assertThat(s.counter().conflictsIfAssigned(1, at(0, 0))).isEqualTo(1);
    }

    @Test
    @DisplayName("counts every distinct partner, which is what makes min-conflicts able to rank moves")
    void countsMultiplePartners() {
        Setup s = setup(4, SolverFixture.clique(4));
        s.counter().assign(0, at(0, 0));
        s.counter().assign(1, at(0, 1));
        s.counter().assign(2, at(1, 0));

        // Hour 0 collides with events 0 and 1; hour 1 collides only with event 2.
        assertThat(s.counter().conflictsIfAssigned(3, at(0, 0))).isEqualTo(2);
        assertThat(s.counter().conflictsIfAssigned(3, at(1, 1))).isEqualTo(1);
        assertThat(s.counter().conflictsIfAssigned(3, at(2, 0))).isZero();
    }

    @Test
    @DisplayName("an event's own current placement is ignored when evaluating a move for it")
    void ignoresOwnAssignmentWhenEvaluating() {
        Setup s = setup(1, SolverFixture.noConflicts(1));
        s.counter().assign(0, at(0, 0));

        // Asking "what if event 0 went here?" must not count event 0 against itself.
        assertThat(s.counter().conflictsIfAssigned(0, at(0, 0))).isZero();
    }

    @Test
    @DisplayName("unassign frees the room again")
    void unassignReleasesOccupancy() {
        Setup s = setup(2, SolverFixture.noConflicts(2));
        s.counter().assign(0, at(0, 0));
        assertThat(s.counter().conflictsIfAssigned(1, at(0, 0))).isEqualTo(1);

        s.counter().unassign(0);
        assertThat(s.counter().conflictsIfAssigned(1, at(0, 0))).isZero();
    }

    @Test
    @DisplayName("cost counts each clashing pair once and tracks unassigned events")
    void costReflectsPairsAndUnassigned() {
        Setup s = setup(3, SolverFixture.noConflicts(3));
        s.counter().assign(0, at(0, 0));
        s.counter().assign(1, at(0, 0));   // clashes with 0; event 2 left unplaced

        SolutionCost cost = s.counter().cost();
        assertThat(cost.conflictingPairs()).isEqualTo(1);
        assertThat(cost.unassignedEvents()).isEqualTo(1);
        assertThat(cost.isFeasible()).isFalse();
        assertThat(cost.total()).isEqualTo(SolutionCost.WEIGHT_UNASSIGNED + SolutionCost.WEIGHT_CONFLICT_PAIR);
    }

    @Test
    @DisplayName("moving an event out of a clash clears the conflict for both sides")
    void movingAwayClearsBothSides() {
        Setup s = setup(2, SolverFixture.clique(2));
        s.counter().assign(0, at(0, 0));
        s.counter().assign(1, at(0, 0));
        assertThat(s.counter().hasConflicts()).isTrue();

        s.counter().assign(1, at(2, 1));  // move event 1 away

        // The partner's count must drop too — if only the moved event were recomputed, the search
        // would keep chasing a conflict that no longer exists.
        assertThat(s.counter().hasConflicts()).isFalse();
        assertThat(s.counter().cost().isFeasible()).isTrue();
    }

    @Test
    @DisplayName("breakdown attributes clashes to the right rule")
    void breakdownSplitsByReason() {
        Setup s = setup(4, new int[][]{{}, {}, {3}, {2}});
        s.counter().assign(0, at(0, 0));
        s.counter().assign(1, at(0, 0));   // room clash (no edge between 0 and 1)
        s.counter().assign(2, at(2, 0));
        s.counter().assign(3, at(2, 1));   // same time, different room, but they are neighbours

        ConflictCounter.CostBreakdown breakdown = s.counter().breakdown();
        assertThat(breakdown.roomClashes()).isEqualTo(1);
        assertThat(breakdown.lecturerStudentClashes()).isEqualTo(1);
        assertThat(breakdown.unassignedEvents()).isZero();
        assertThat(breakdown.conflictingEventIds()).hasSize(4);
    }
}
