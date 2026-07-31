package com.university.timetable_scheduler.solver;

import java.util.Arrays;

/**
 * A complete (or partial) timetable: for each event index, which candidate from its domain it took.
 *
 * <p>That's it — one {@code int} per event. Copying a whole timetable is an array clone, which
 * matters because every ant, on every iteration, starts from a fresh one.
 */
public final class Solution {

    /** Sentinel for "this event has no assignment yet" (or has no legal assignment at all). */
    public static final int UNASSIGNED = -1;

    private final int[] choice;
    private SolutionCost cost;

    private Solution(int[] choice) {
        this.choice = choice;
    }

    /** A solution with nothing assigned yet — the starting point for an ant's construction. */
    public static Solution empty(int eventCount) {
        int[] choice = new int[eventCount];
        Arrays.fill(choice, UNASSIGNED);
        return new Solution(choice);
    }

    public int choiceOf(int event)              { return choice[event]; }
    public boolean isAssigned(int event)        { return choice[event] != UNASSIGNED; }
    public int eventCount()                     { return choice.length; }
    public int[] rawChoices()                   { return choice; }

    /** Setting a choice invalidates the cached cost — it is recomputed on next {@link #cost()}. */
    public void setChoice(int event, int candidate) {
        choice[event] = candidate;
        cost = null;
    }

    public SolutionCost cost()                  { return cost; }
    public void setCost(SolutionCost cost)      { this.cost = cost; }

    public Solution copy() {
        Solution c = new Solution(choice.clone());
        c.cost = this.cost;
        return c;
    }
}
