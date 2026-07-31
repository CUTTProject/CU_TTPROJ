package com.university.timetable_scheduler.solver;

import com.university.timetable_scheduler.entity.Event;
import com.university.timetable_scheduler.entity.Room;
import com.university.timetable_scheduler.entity.Section;
import com.university.timetable_scheduler.entity.Timeslot;
import com.university.timetable_scheduler.repository.*;
import com.university.timetable_scheduler.status.TimeslotEnum;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Turns the database into a {@link CspModel} — the spec's §1 (input &amp; pre-processing), §2
 * (conflict graph) and §3 (variables, domains, constraints).
 *
 * <p>All the entity handling and index construction lives here so the solver classes never touch
 * JPA and can be reasoned about (and tested) as plain algorithms over {@code int}s.
 */
@Component
@AllArgsConstructor
public class CspModelBuilder {

    private static final Logger log = LoggerFactory.getLogger(CspModelBuilder.class);

    private final EventRepository eventRepository;
    private final TimeslotRepository timeslotRepository;
    private final RoomRepository roomRepository;
    private final SectionRepository sectionRepository;
    private final SectionRoomRepository sectionRoomRepository;
    private final ConflictGraphBuilder conflictGraphBuilder;

    /**
     * Builds the CSP for one academic period.
     *
     * <p><b>Scoping to a period is a bug fix, not a refactor.</b> The previous solver loaded events
     * with {@code findEventByFilter(schoolId, null, null, null, null)} — a query with no
     * academic-period parameter at all — so generating Fall's timetable rescheduled and persisted
     * every event in Spring and every archived period too. The caller never noticed because
     * {@code generateTimetable} reads results back scoped to a single period.
     *
     * @return empty when there is nothing to solve (no events, rooms, or timeslots)
     */
    public Optional<CspModel> build(UUID schoolId, UUID academicPeriodId) {

        List<Event> events = eventRepository.findAllBySchoolIdAndAcademicPeriodId(schoolId, academicPeriodId);
        List<Room> rooms = roomRepository.findAllBySchool_Id(schoolId);
        List<Timeslot> slots = usableTimeslots(schoolId);
        List<Section> sections = sectionRepository
                .findSectionByFilter(schoolId, null, null, null, academicPeriodId, null);

        if (events.isEmpty() || rooms.isEmpty() || slots.isEmpty()) {
            log.warn("Nothing to solve for period {}: {} events, {} rooms, {} timeslots",
                    academicPeriodId, events.size(), rooms.size(), slots.size());
            return Optional.empty();
        }

        Map<UUID, Integer> eventIndex = indexById(events, Event::getId);
        Map<UUID, Integer> roomIndex = indexById(rooms, Room::getId);
        Map<UUID, Integer> slotIndex = indexById(slots, Timeslot::getId);

        int[][] conflictNeighbours = buildEventConflicts(events, sections, eventIndex);
        int[][] overlappingSlots = buildSlotOverlapIndex(slots);
        List<List<Candidate>> domains = buildDomains(events, rooms, slots, roomIndex, slotIndex, schoolId);
        int[] searchOrder = buildSearchOrder(domains, conflictNeighbours);

        logSummary(events, rooms, slots, domains, conflictNeighbours);

        return Optional.of(new CspModel(events, eventIndex, domains, conflictNeighbours,
                rooms, slots, overlappingSlots, searchOrder));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // §1 — Timeslots
    // ─────────────────────────────────────────────────────────────────────────────

    /** Timeslots missing a day or either time cannot be reasoned about, so they are dropped. */
    private List<Timeslot> usableTimeslots(UUID schoolId) {
        return timeslotRepository.findAllBySchool_Id(schoolId).stream()
                .filter(t -> t.getTimeslotDay() != null
                        && t.getTimeslotStartTime() != null
                        && t.getTimeslotEndTime() != null)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // §2 — Conflict graph, expanded from sections down to events
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * The graph is built over sections (two sections conflict if they share a lecturer or students),
     * but the CSP's variables are events. So every event of section A inherits an edge to every
     * event of each section conflicting with A — and, critically, to its own siblings: two events of
     * the same section share both lecturer and students by definition and must not overlap either.
     */
    private int[][] buildEventConflicts(List<Event> events, List<Section> sections,
                                        Map<UUID, Integer> eventIndex) {

        Map<UUID, Set<UUID>> sectionNeighbours = conflictGraphBuilder.build(sections).toNeighbourMap();

        Map<UUID, List<Event>> eventsBySection = events.stream()
                .filter(e -> e.getEventSection() != null)
                .collect(Collectors.groupingBy(e -> e.getEventSection().getId()));

        int[][] neighbours = new int[events.size()][];

        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            if (event.getEventSection() == null) {
                neighbours[i] = new int[0];
                continue;
            }
            UUID sectionId = event.getEventSection().getId();
            Set<Integer> conflicting = new HashSet<>();

            // Events of conflicting sections.
            for (UUID neighbourSection : sectionNeighbours.getOrDefault(sectionId, Set.of())) {
                for (Event other : eventsBySection.getOrDefault(neighbourSection, List.of())) {
                    Integer index = eventIndex.get(other.getId());
                    if (index != null && index != i) conflicting.add(index);
                }
            }

            // Sibling events of the same section — same students, same lecturer.
            for (Event sibling : eventsBySection.getOrDefault(sectionId, List.of())) {
                Integer index = eventIndex.get(sibling.getId());
                if (index != null && index != i) conflicting.add(index);
            }

            neighbours[i] = conflicting.stream().mapToInt(Integer::intValue).toArray();
        }
        return neighbours;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Slot overlap index
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * For each slot, every slot whose interval intersects it (itself included).
     *
     * <p>Normally slots are disjoint and each entry is just {@code {s}}, making the room-occupancy
     * check a single lookup. But a school may legitimately define overlapping timeslot rows — a
     * 9–11 slot alongside a 10–12 one — and without this, two blocks built from different rows
     * would look conflict-free while actually colliding in the same room.
     */
    private int[][] buildSlotOverlapIndex(List<Timeslot> slots) {
        int[][] index = new int[slots.size()][];

        for (int i = 0; i < slots.size(); i++) {
            Timeslot a = slots.get(i);
            List<Integer> overlapping = new ArrayList<>();
            for (int j = 0; j < slots.size(); j++) {
                Timeslot b = slots.get(j);
                if (i == j || intervalsIntersect(a, b)) overlapping.add(j);
            }
            index[i] = overlapping.stream().mapToInt(Integer::intValue).toArray();
        }
        return index;
    }

    private boolean intervalsIntersect(Timeslot a, Timeslot b) {
        return a.getTimeslotDay() == b.getTimeslotDay()
                && a.getTimeslotStartTime().isBefore(b.getTimeslotEndTime())
                && a.getTimeslotEndTime().isAfter(b.getTimeslotStartTime());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // §3 — Domains: every legal (t, r) per event, precomputed
    // ─────────────────────────────────────────────────────────────────────────────

    private List<List<Candidate>> buildDomains(List<Event> events, List<Room> rooms,
                                               List<Timeslot> slots,
                                               Map<UUID, Integer> roomIndex,
                                               Map<UUID, Integer> slotIndex,
                                               UUID schoolId) {

        Map<TimeslotEnum.TimeslotDay, List<Timeslot>> slotsByDay = slots.stream()
                .collect(Collectors.groupingBy(Timeslot::getTimeslotDay));
        slotsByDay.values().forEach(list -> list.sort(Comparator.comparing(Timeslot::getTimeslotStartTime)));

        // Blocks depend only on duration, so cache per distinct duration rather than per event.
        Map<Duration, List<TimeslotBlock>> blocksByDuration = new HashMap<>();
        Map<UUID, List<Room>> allowedRoomsBySection = loadAllowedRooms(schoolId);

        List<List<Candidate>> domains = new ArrayList<>(events.size());

        for (Event event : events) {
            List<TimeslotBlock> blocks = blocksByDuration.computeIfAbsent(
                    event.getEventDuration(), duration -> buildContiguousBlocks(slotsByDay, duration));

            List<Room> allowedRooms = rooms;
            if (event.getEventSection() != null) {
                List<Room> restricted = allowedRoomsBySection.get(event.getEventSection().getId());
                if (restricted != null && !restricted.isEmpty()) allowedRooms = restricted;
            }

            List<Candidate> domain = new ArrayList<>(blocks.size() * allowedRooms.size());
            for (TimeslotBlock block : blocks) {
                int[] blockSlotIndices = block.timeslots().stream()
                        .mapToInt(t -> slotIndex.get(t.getId()))
                        .toArray();

                for (Room room : allowedRooms) {
                    Integer roomIdx = roomIndex.get(room.getId());
                    if (roomIdx == null) continue;

                    // NOTE: room capacity vs. section enrollment is NOT filtered here. The spec
                    // (§3, "Constraints on suitability") requires it, but it is deliberately out of
                    // scope while the data is auto-generated. To enforce it, skip rooms whose
                    // roomCapacity is below the section's enrollment size — the domain shrinks and
                    // nothing else in the solver needs to change.
                    domain.add(new Candidate(block, room, roomIdx, blockSlotIndices));
                }
            }
            domains.add(domain);
        }
        return domains;
    }

    /** sectionId → the rooms it is restricted to (spec §1.i, "a set of suitable rooms"). */
    private Map<UUID, List<Room>> loadAllowedRooms(UUID schoolId) {
        Map<UUID, List<Room>> allowed = new HashMap<>();
        sectionRoomRepository.findAllBySchool_Id(schoolId).forEach(sr -> {
            if (sr.getSectionRoomSection() != null && sr.getSectionRoom() != null) {
                allowed.computeIfAbsent(sr.getSectionRoomSection().getId(), k -> new ArrayList<>())
                        .add(sr.getSectionRoom());
            }
        });
        return allowed;
    }

    /**
     * Every contiguous run of slots on one day whose durations sum to exactly {@code needed}.
     *
     * <p>Contiguity means slot[j-1] ends exactly when slot[j] starts — a gap breaks the run.
     *
     * <p><b>The match is exact.</b> A 90-minute event against 1-hour slots therefore produces no
     * blocks, an empty domain, and an event no algorithm can place. Rather than let the solver
     * grind on that forever (the old loop burned all 10,000 iterations on it, silently), such
     * events are excluded from the search and reported via
     * {@link CspModel#structurallyUnschedulableEvents()}. Durations are supplied per-row by the CSV
     * upload, so this surfaces as a data problem where it belongs.
     */
    private List<TimeslotBlock> buildContiguousBlocks(Map<TimeslotEnum.TimeslotDay, List<Timeslot>> slotsByDay,
                                                      Duration needed) {
        List<TimeslotBlock> blocks = new ArrayList<>();
        if (needed == null || needed.isZero() || needed.isNegative()) return blocks;

        for (List<Timeslot> daySlots : slotsByDay.values()) {
            for (int start = 0; start < daySlots.size(); start++) {
                List<Timeslot> run = new ArrayList<>();
                Duration accumulated = Duration.ZERO;

                for (int j = start; j < daySlots.size(); j++) {
                    Timeslot slot = daySlots.get(j);

                    if (j > start && !daySlots.get(j - 1).getTimeslotEndTime()
                            .equals(slot.getTimeslotStartTime())) {
                        break;  // gap — the run ends here
                    }

                    run.add(slot);
                    accumulated = accumulated.plus(durationOf(slot));

                    if (accumulated.equals(needed)) {
                        blocks.add(new TimeslotBlock(List.copyOf(run)));
                        break;
                    }
                    if (accumulated.compareTo(needed) > 0) break;  // overshot
                }
            }
        }
        return blocks;
    }

    /** Falls back to the slot's own start/end when timeslotDuration was never populated. */
    private Duration durationOf(Timeslot slot) {
        if (slot.getTimeslotDuration() != null && !slot.getTimeslotDuration().isZero()) {
            return slot.getTimeslotDuration();
        }
        return Duration.between(slot.getTimeslotStartTime(), slot.getTimeslotEndTime());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Search order
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Most-constrained-first (the CSP "minimum remaining values" heuristic): fewest candidates
     * first, ties broken by highest conflict-graph degree.
     *
     * <p>Ants assign in this order so the events with the fewest options get placed while the
     * timetable is still mostly empty. Leaving them until last is how you end up with an event
     * that has nowhere legal to go.
     *
     * <p>Empty-domain events are excluded outright — there is nothing to choose for them.
     */
    private int[] buildSearchOrder(List<List<Candidate>> domains, int[][] conflictNeighbours) {
        return java.util.stream.IntStream.range(0, domains.size())
                .filter(e -> !domains.get(e).isEmpty())
                .boxed()
                .sorted(Comparator
                        .comparingInt((Integer e) -> domains.get(e).size())
                        .thenComparing(e -> -conflictNeighbours[e].length))
                .mapToInt(Integer::intValue)
                .toArray();
    }

    // ─────────────────────────────────────────────────────────────────────────────

    private <T> Map<UUID, Integer> indexById(List<T> items, java.util.function.Function<T, UUID> idOf) {
        Map<UUID, Integer> index = new HashMap<>();
        for (int i = 0; i < items.size(); i++) index.put(idOf.apply(items.get(i)), i);
        return index;
    }

    private void logSummary(List<Event> events, List<Room> rooms, List<Timeslot> slots,
                            List<List<Candidate>> domains, int[][] conflictNeighbours) {
        int edges = Arrays.stream(conflictNeighbours).mapToInt(n -> n.length).sum() / 2;
        long emptyDomains = domains.stream().filter(List::isEmpty).count();
        log.info("CSP built: {} events, {} rooms, {} timeslots, {} conflict edges, "
                        + "avg domain {}, {} event(s) with an empty domain",
                events.size(), rooms.size(), slots.size(), edges,
                String.format("%.1f", domains.stream().mapToInt(List::size).average().orElse(0)),
                emptyDomains);
    }
}
