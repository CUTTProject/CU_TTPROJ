package com.university.timetable_scheduler.solver;

import com.university.timetable_scheduler.entity.Event;
import com.university.timetable_scheduler.entity.Room;
import com.university.timetable_scheduler.entity.Timeslot;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The immutable CSP instance the solver reasons about — the triple {@code (X, D, Constraints)}
 * from the spec, flattened into index-addressed arrays.
 *
 * <p><b>Everything here is dense-indexed.</b> Events are {@code 0..eventCount-1}, and an event's
 * domain values are {@code 0..domainOf(e).size()-1}. A whole timetable is therefore just an
 * {@code int[]} (see {@link Solution}), and the pheromone table is a {@code double[][]} — which is
 * what makes hundreds of thousands of ant constructions affordable inside the time budget.
 *
 * <p>Mapping back to the spec:
 * <ul>
 *   <li><b>Variables (X)</b> — one per event; the index {@code 0..eventCount-1}.</li>
 *   <li><b>Domains (D)</b> — {@link #domainOf(int)}, precomputed once as the spec requires
 *       ("Di must be precomputed for each ei to reduce the search space").</li>
 *   <li><b>Binary colouring constraint</b> — {@link #neighboursOf(int)}, the conflict graph
 *       expanded from sections down to individual events.</li>
 *   <li><b>Room exclusivity</b> — not stored here; it is implied for <em>every</em> pair of events
 *       and is enforced by {@link ConflictCounter} via the room occupancy index.</li>
 * </ul>
 */
public final class CspModel {

    private final List<Event> events;
    private final Map<UUID, Integer> indexByEventId;

    /** domains[e] = every legal (timeslotBlock, room) pair for event e. */
    private final List<List<Candidate>> domains;

    /** neighboursOf[e] = events that may not overlap e in time (shared lecturer or students). */
    private final int[][] conflictNeighbours;

    private final List<Room> rooms;
    private final List<Timeslot> slots;

    /**
     * overlappingSlots[s] = every slot index whose time interval intersects slot s, including s.
     *
     * <p>For the usual case of disjoint slots this is just {@code {s}}, and the occupancy checks
     * collapse to a single lookup. It exists because a school is free to define overlapping
     * timeslot rows (a 9–11 slot alongside a 10–12 slot); without this, two blocks built from
     * different-but-overlapping rows would look conflict-free when they are not.
     */
    private final int[][] overlappingSlots;

    /**
     * The order ants assign variables in: most-constrained-first (smallest domain, then highest
     * conflict-graph degree). This is the classic CSP MRV heuristic — commit to the events with
     * the fewest escape routes while the timetable is still empty enough to place them.
     */
    private final int[] searchOrder;

    CspModel(List<Event> events,
             Map<UUID, Integer> indexByEventId,
             List<List<Candidate>> domains,
             int[][] conflictNeighbours,
             List<Room> rooms,
             List<Timeslot> slots,
             int[][] overlappingSlots,
             int[] searchOrder) {
        this.events = events;
        this.indexByEventId = indexByEventId;
        this.domains = domains;
        this.conflictNeighbours = conflictNeighbours;
        this.rooms = rooms;
        this.slots = slots;
        this.overlappingSlots = overlappingSlots;
        this.searchOrder = searchOrder;
    }

    public int eventCount()                  { return events.size(); }
    public List<Event> events()              { return events; }
    public Event event(int e)                { return events.get(e); }
    public List<Candidate> domainOf(int e)   { return domains.get(e); }
    public Candidate candidate(int e, int k) { return domains.get(e).get(k); }
    public int[] neighboursOf(int e)         { return conflictNeighbours[e]; }
    public List<Room> rooms()                { return rooms; }
    public List<Timeslot> slots()            { return slots; }
    public int roomCount()                   { return rooms.size(); }
    public int slotCount()                   { return slots.size(); }
    public int[] overlappingSlots(int slot)  { return overlappingSlots[slot]; }
    public int[] searchOrder()               { return searchOrder; }

    public Integer indexOf(UUID eventId)     { return indexByEventId.get(eventId); }

    /** Events with an empty domain can never be scheduled; reported rather than retried forever. */
    public List<Integer> structurallyUnschedulableEvents() {
        return java.util.stream.IntStream.range(0, eventCount())
                .filter(e -> domains.get(e).isEmpty())
                .boxed()
                .toList();
    }

    public double averageDomainSize() {
        return domains.stream().mapToInt(List::size).average().orElse(1.0);
    }
}
