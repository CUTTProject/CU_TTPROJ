package com.university.timetable_scheduler.solver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Counts hard-constraint violations, and maintains that count incrementally as events move.
 *
 * <p><b>This class is the spec's {@code CONFLICTS(csp, var, v, current)} (page 4, Note 1)</b> — the
 * function the previous implementation never had. The old code asked only "is this candidate
 * <em>completely</em> clean, yes or no?" ({@code isConstraintFree}), which is a boolean, so when no
 * clean slot existed it fell back to a <em>random</em> violating one. A search cannot descend a
 * gradient it never computes. {@link #conflictsIfAssigned} returns the actual count, so
 * min-conflicts can pick the least-bad move, and the ants get a meaningful heuristic.
 *
 * <p><b>Why it is fast.</b> The old check scanned every event in the school for every candidate.
 * Here, two indices make it proportional to a candidate's own footprint instead:
 * <ul>
 *   <li><i>Room exclusivity</i> — a {@code (room, slot) -> events} occupancy table, so finding who
 *       else is in this room at this time is a lookup, not a scan.</li>
 *   <li><i>Colouring</i> — only the event's conflict-graph neighbours are examined, and that list
 *       is short.</li>
 * </ul>
 *
 * <p><b>Statefulness.</b> An instance owns a {@link Solution} and mutates alongside it. Call
 * {@link #assign}/{@link #unassign} rather than touching the Solution directly, or the indices
 * drift out of sync with the assignment.
 *
 * <p>Not thread-safe. Each ant builds with its own instance.
 */
public final class ConflictCounter {

    private final CspModel model;
    private final Solution solution;

    /** roomSlotOccupants[roomIndex * slotCount + slotIndex] = events booking that room then. */
    private final Set<Integer>[] roomSlotOccupants;

    /** conflictCount[e] = how many distinct other events e currently violates a constraint with. */
    private final int[] conflictCount;

    /** The events with conflictCount > 0 — min-conflicts picks its next variable from here. */
    private final IndexedEventSet conflicted;

    /** Reused by {@link #conflictsIfAssigned} to keep the hot path allocation-free. */
    private final Set<Integer> scratch = new HashSet<>();

    @SuppressWarnings("unchecked")
    public ConflictCounter(CspModel model, Solution solution) {
        this.model = model;
        this.solution = solution;
        this.roomSlotOccupants = new Set[model.roomCount() * model.slotCount()];
        this.conflictCount = new int[model.eventCount()];
        this.conflicted = new IndexedEventSet(model.eventCount());
        rebuildFrom(solution);
    }

    public Solution solution() {
        return solution;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // The core question: CONFLICTS(csp, var, v, current)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * How many hard-constraint violations would result from giving event {@code e} candidate
     * {@code k}, against everything currently placed. Zero means a clean placement.
     *
     * <p>{@code e}'s own current assignment is ignored, so this answers "if I moved e here"
     * without needing to unassign it first.
     */
    public int conflictsIfAssigned(int e, int k) {
        scratch.clear();
        collectPartners(e, k, scratch);
        return scratch.size();
    }

    /**
     * Finds every event that would violate a hard constraint with {@code e} placed at {@code k}.
     *
     * <p>A pair is collected at most once even when it breaks both rules at the same time (same
     * room <em>and</em> shared lecturer) — {@code out} is a set of event indices, so the two
     * sources below naturally union. That distinctness is what {@link SolutionCost} assumes.
     */
    private void collectPartners(int e, int k, Set<Integer> out) {
        Candidate cand = model.candidate(e, k);

        // Rule 1 — Room exclusivity: nobody else may hold this room while this block runs.
        // Applies to every pair of events, conflict-graph neighbours or not.
        int roomBase = cand.roomIndex() * model.slotCount();
        for (int slot : cand.slotIndices()) {
            // overlappingSlots covers schools whose timeslot rows themselves overlap; for the
            // ordinary disjoint case this loop runs exactly once.
            for (int overlapping : model.overlappingSlots(slot)) {
                Set<Integer> occupants = roomSlotOccupants[roomBase + overlapping];
                if (occupants == null) continue;
                for (int other : occupants) {
                    if (other != e) out.add(other);
                }
            }
        }

        // Rule 2 — Colouring: adjacent events in the conflict graph may not overlap in time.
        // (Shared lecturer or shared students; the edge already encodes the reason.)
        for (int neighbour : model.neighboursOf(e)) {
            if (neighbour == e || !solution.isAssigned(neighbour)) continue;
            Candidate other = model.candidate(neighbour, solution.choiceOf(neighbour));
            if (other.block().overlapsWith(cand.block())) out.add(neighbour);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Mutation — keeps the occupancy index and conflict counts in step
    // ─────────────────────────────────────────────────────────────────────────────

    /** Places {@code e} at candidate {@code k}, moving it if it was already placed. */
    public void assign(int e, int k) {
        if (solution.isAssigned(e)) unassign(e);

        solution.setChoice(e, k);
        addToOccupancy(e, k);

        // Only e and its new partners can have changed; recompute exactly those.
        Set<Integer> affected = new HashSet<>();
        collectPartners(e, k, affected);
        affected.add(e);
        affected.forEach(this::recomputeConflictCount);
    }

    /** Removes {@code e}'s placement, if any. */
    public void unassign(int e) {
        if (!solution.isAssigned(e)) return;

        int k = solution.choiceOf(e);

        // Capture partners before the occupancy index forgets e.
        Set<Integer> affected = new HashSet<>();
        collectPartners(e, k, affected);

        removeFromOccupancy(e, k);
        solution.setChoice(e, Solution.UNASSIGNED);

        setConflictCount(e, 0);
        affected.forEach(this::recomputeConflictCount);
    }

    private void addToOccupancy(int e, int k) {
        Candidate cand = model.candidate(e, k);
        int roomBase = cand.roomIndex() * model.slotCount();
        for (int slot : cand.slotIndices()) {
            int cell = roomBase + slot;
            Set<Integer> occupants = roomSlotOccupants[cell];
            if (occupants == null) roomSlotOccupants[cell] = occupants = new HashSet<>(2);
            occupants.add(e);
        }
    }

    private void removeFromOccupancy(int e, int k) {
        Candidate cand = model.candidate(e, k);
        int roomBase = cand.roomIndex() * model.slotCount();
        for (int slot : cand.slotIndices()) {
            Set<Integer> occupants = roomSlotOccupants[roomBase + slot];
            if (occupants != null) occupants.remove(e);
        }
    }

    private void recomputeConflictCount(int e) {
        if (!solution.isAssigned(e)) {
            setConflictCount(e, 0);
            return;
        }
        scratch.clear();
        collectPartners(e, solution.choiceOf(e), scratch);
        setConflictCount(e, scratch.size());
    }

    private void setConflictCount(int e, int count) {
        conflictCount[e] = count;
        if (count > 0) conflicted.add(e);
        else conflicted.remove(e);
    }

    private void rebuildFrom(Solution s) {
        for (int e = 0; e < model.eventCount(); e++) {
            if (s.isAssigned(e)) addToOccupancy(e, s.choiceOf(e));
        }
        for (int e = 0; e < model.eventCount(); e++) {
            recomputeConflictCount(e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Reporting
    // ─────────────────────────────────────────────────────────────────────────────

    /** Events currently involved in at least one violation. Allocates — for reporting, not the loop. */
    public List<Integer> conflictedEvents() {
        return conflicted.toList();
    }

    /**
     * A uniformly random conflicted event — the flowchart's "Var :- A randomly chosen variable".
     * O(1) and allocation-free; callers must check {@link #hasConflicts()} first.
     */
    public int randomConflictedEvent(java.util.Random random) {
        return conflicted.randomMember(random);
    }

    public int conflictedEventCount() {
        return conflicted.size();
    }

    /** True while any hard constraint is still violated — the flowchart's solution test. */
    public boolean hasConflicts() {
        return !conflicted.isEmpty();
    }

    /**
     * The current cost.
     *
     * <p>Events with an empty domain are excluded from {@code unassignedEvents}: no move can ever
     * place them, so counting them would hold the cost permanently above zero and deny the solver
     * its early exit — burning the full time budget on an unwinnable game. They are surfaced to the
     * caller via {@link CspModel#structurallyUnschedulableEvents()} instead of being retried.
     */
    public SolutionCost cost() {
        int unassigned = 0;
        for (int e = 0; e < model.eventCount(); e++) {
            if (model.domainOf(e).isEmpty()) continue;
            if (!solution.isAssigned(e)) unassigned++;
        }

        // Each conflicting pair is counted by both of its members, hence the halving.
        int degreeSum = 0;
        for (int count : conflictCount) degreeSum += count;

        return new SolutionCost(unassigned, degreeSum / 2);
    }

    /**
     * The human-facing breakdown, split by violation reason.
     *
     * <p>Deliberately separate from {@link #cost()} and run once at the end: categorising every
     * pair costs more than the search needs, since both reasons carry the same weight anyway.
     * A pair violating both rules is attributed to the room clash, matching the previous
     * implementation's precedence.
     */
    public CostBreakdown breakdown() {
        int roomClashes = 0;
        int lecturerStudentClashes = 0;
        int unassigned = 0;
        Set<UUID> conflictingEventIds = new HashSet<>();
        Set<Long> countedPairs = new HashSet<>();

        for (int e = 0; e < model.eventCount(); e++) {
            if (!solution.isAssigned(e)) {
                if (!model.domainOf(e).isEmpty()) {
                    unassigned++;
                    conflictingEventIds.add(model.event(e).getId());
                }
                continue;
            }

            Candidate cand = model.candidate(e, solution.choiceOf(e));
            Set<Integer> partners = new HashSet<>();
            collectPartners(e, solution.choiceOf(e), partners);

            for (int other : partners) {
                long pairKey = pairKey(e, other);
                if (!countedPairs.add(pairKey)) continue;

                Candidate otherCand = model.candidate(other, solution.choiceOf(other));
                if (otherCand.roomIndex() == cand.roomIndex()) roomClashes++;
                else lecturerStudentClashes++;

                conflictingEventIds.add(model.event(e).getId());
                conflictingEventIds.add(model.event(other).getId());
            }
        }
        return new CostBreakdown(unassigned, roomClashes, lecturerStudentClashes, conflictingEventIds);
    }

    /** Order-independent key for an unordered pair, so (a,b) and (b,a) collapse to one entry. */
    private static long pairKey(int a, int b) {
        int lo = Math.min(a, b);
        int hi = Math.max(a, b);
        return ((long) lo << 32) | (hi & 0xFFFFFFFFL);
    }

    /** Violation counts split by reason, for logs and the API response. */
    public record CostBreakdown(int unassignedEvents,
                                int roomClashes,
                                int lecturerStudentClashes,
                                Set<UUID> conflictingEventIds) {}
}
