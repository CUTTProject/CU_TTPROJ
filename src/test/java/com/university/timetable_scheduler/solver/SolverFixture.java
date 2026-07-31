package com.university.timetable_scheduler.solver;

import com.university.timetable_scheduler.entity.Event;
import com.university.timetable_scheduler.entity.Room;
import com.university.timetable_scheduler.entity.Timeslot;
import com.university.timetable_scheduler.status.TimeslotEnum;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

/**
 * Builds {@link CspModel} instances in memory, with no database and no Spring context.
 *
 * <p>The solver is a plain algorithm over {@code int}s, so its tests should be too — this keeps
 * them fast enough to assert on real 10-second solver runs.
 */
final class SolverFixture {

    private SolverFixture() {}

    static Timeslot slot(TimeslotEnum.TimeslotDay day, int hour) {
        Timeslot t = new Timeslot();
        t.setId(UUID.randomUUID());
        t.setTimeslotDay(day);
        t.setTimeslotStartTime(LocalTime.of(hour, 0));
        t.setTimeslotEndTime(LocalTime.of(hour + 1, 0));
        t.setTimeslotDuration(Duration.ofHours(1));
        return t;
    }

    static Room room(String number) {
        Room r = new Room();
        r.setId(UUID.randomUUID());
        r.setRoomNumber(number);
        return r;
    }

    static Event event(Duration duration) {
        Event e = new Event();
        e.setId(UUID.randomUUID());
        e.setEventDuration(duration);
        return e;
    }

    /** {@code hoursPerDay} 1-hour slots on each of the first {@code days} weekdays. */
    static List<Timeslot> slots(int days, int hoursPerDay) {
        TimeslotEnum.TimeslotDay[] weekdays = {
                TimeslotEnum.TimeslotDay.MONDAY, TimeslotEnum.TimeslotDay.TUESDAY,
                TimeslotEnum.TimeslotDay.WEDNESDAY, TimeslotEnum.TimeslotDay.THURSDAY,
                TimeslotEnum.TimeslotDay.FRIDAY};

        List<Timeslot> result = new ArrayList<>();
        for (int d = 0; d < days; d++) {
            for (int h = 0; h < hoursPerDay; h++) result.add(slot(weekdays[d], 9 + h));
        }
        return result;
    }

    static List<Room> rooms(int count) {
        List<Room> result = new ArrayList<>();
        for (int i = 0; i < count; i++) result.add(room("R" + i));
        return result;
    }

    /**
     * Assembles a model directly.
     *
     * <p>Mirrors what {@link CspModelBuilder} produces, but from plain arguments: this exercises
     * the <em>algorithm</em> without dragging JPA in. Slots are disjoint 1-hour blocks, so the
     * overlap index is the identity.
     *
     * @param adjacency adjacency[e] = events that may not overlap e in time (must be symmetric)
     */
    static CspModel model(List<Event> events, List<Room> rooms, List<Timeslot> slots, int[][] adjacency) {
        Map<UUID, Integer> eventIndex = new HashMap<>();
        for (int i = 0; i < events.size(); i++) eventIndex.put(events.get(i).getId(), i);

        Map<UUID, Integer> slotIndex = new HashMap<>();
        for (int i = 0; i < slots.size(); i++) slotIndex.put(slots.get(i).getId(), i);

        List<List<Candidate>> domains = new ArrayList<>();
        for (Event event : events) {
            domains.add(domainFor(event, rooms, slots, slotIndex));
        }

        // Disjoint slots: each overlaps only itself.
        int[][] overlappingSlots = new int[slots.size()][];
        for (int i = 0; i < slots.size(); i++) overlappingSlots[i] = new int[]{i};

        int[] searchOrder = java.util.stream.IntStream.range(0, events.size())
                .filter(e -> !domains.get(e).isEmpty())
                .boxed()
                .sorted(Comparator.comparingInt(e -> domains.get(e).size()))
                .mapToInt(Integer::intValue)
                .toArray();

        return new CspModel(events, eventIndex, domains, adjacency, rooms, slots,
                overlappingSlots, searchOrder);
    }

    /** Every contiguous same-day run of slots matching the event's duration, times every room. */
    private static List<Candidate> domainFor(Event event, List<Room> rooms, List<Timeslot> slots,
                                             Map<UUID, Integer> slotIndex) {
        List<Candidate> domain = new ArrayList<>();
        long hoursNeeded = event.getEventDuration().toHours();

        // Exact match only — a 90-minute event against 1-hour slots yields nothing, which is
        // precisely the "structurally unschedulable" case the solver reports.
        if (event.getEventDuration().toMinutes() % 60 != 0 || hoursNeeded == 0) return domain;

        for (int start = 0; start + hoursNeeded <= slots.size(); start++) {
            List<Timeslot> run = slots.subList(start, (int) (start + hoursNeeded));

            boolean contiguous = true;
            for (int j = 1; j < run.size(); j++) {
                if (run.get(j - 1).getTimeslotDay() != run.get(j).getTimeslotDay()
                        || !run.get(j - 1).getTimeslotEndTime().equals(run.get(j).getTimeslotStartTime())) {
                    contiguous = false;
                    break;
                }
            }
            if (!contiguous) continue;

            TimeslotBlock block = new TimeslotBlock(List.copyOf(run));
            int[] indices = run.stream().mapToInt(t -> slotIndex.get(t.getId())).toArray();

            for (int r = 0; r < rooms.size(); r++) {
                domain.add(new Candidate(block, rooms.get(r), r, indices));
            }
        }
        return domain;
    }

    /** Adjacency where every listed event conflicts with every other — a clique. */
    static int[][] clique(int size) {
        int[][] adjacency = new int[size][];
        for (int i = 0; i < size; i++) {
            int[] neighbours = new int[size - 1];
            int n = 0;
            for (int j = 0; j < size; j++) if (j != i) neighbours[n++] = j;
            adjacency[i] = neighbours;
        }
        return adjacency;
    }

    static int[][] noConflicts(int size) {
        int[][] adjacency = new int[size][];
        for (int i = 0; i < size; i++) adjacency[i] = new int[0];
        return adjacency;
    }

    /** Defaults tuned for tests: short budget, verbose off. */
    static SolverParameters params(long timeLimitSeconds) {
        SolverParameters p = new SolverParameters();
        p.setTimeLimitSeconds(timeLimitSeconds);
        p.setVerbose(false);
        p.setAnts(6);
        return p;
    }
}
