package com.university.timetable_scheduler.solver;

import java.util.List;
import java.util.Random;

/**
 * A set of event indices with O(1) add, remove, contains <em>and uniform random pick</em>.
 *
 * <p>Exists for one line of the spec's flowchart: "Var :- A randomly chosen variable". Min-conflicts
 * does that every single step, tens of thousands of times per ant. A {@code HashSet} would force
 * either an O(n) walk or a fresh {@code ArrayList} allocation on each pick — the sort of thing that
 * quietly eats a 10-minute budget.
 *
 * <p>Standard dense/sparse trick: {@code dense} holds the members packed into a prefix, and
 * {@code positionInDense} maps an event back to its slot so removal can swap-with-last instead of
 * shifting. Random pick is then just an index into {@code dense}.
 */
final class IndexedEventSet {

    private static final int ABSENT = -1;

    private final int[] dense;
    private final int[] positionInDense;
    private int size;

    IndexedEventSet(int capacity) {
        this.dense = new int[capacity];
        this.positionInDense = new int[capacity];
        java.util.Arrays.fill(positionInDense, ABSENT);
    }

    boolean contains(int event) {
        return positionInDense[event] != ABSENT;
    }

    void add(int event) {
        if (contains(event)) return;
        dense[size] = event;
        positionInDense[event] = size;
        size++;
    }

    void remove(int event) {
        int position = positionInDense[event];
        if (position == ABSENT) return;

        // Move the last member into the hole so dense stays packed.
        int last = dense[size - 1];
        dense[position] = last;
        positionInDense[last] = position;

        positionInDense[event] = ABSENT;
        size--;
    }

    /** Uniformly random member. Caller must check {@link #isEmpty()} first. */
    int randomMember(Random random) {
        return dense[random.nextInt(size)];
    }

    int size()          { return size; }
    boolean isEmpty()   { return size == 0; }

    List<Integer> toList() {
        return java.util.Arrays.stream(dense, 0, size).boxed().toList();
    }
}
