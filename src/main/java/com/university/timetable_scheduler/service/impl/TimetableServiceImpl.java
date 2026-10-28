package com.university.timetable_scheduler.service.impl;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.university.timetable_scheduler.dto.request.timetable.*;
import com.university.timetable_scheduler.dto.response.timetable.SplitDecisionDTO;
import com.university.timetable_scheduler.dto.response.timetable.TimetableEntryDTO;
import com.university.timetable_scheduler.dto.response.timetable.TimetableResponse;
import com.university.timetable_scheduler.dto.response.timetable.UnassignedEventDTO;
import com.university.timetable_scheduler.entity.*;
import com.university.timetable_scheduler.repository.*;
import com.university.timetable_scheduler.service.TimetableService;
import com.university.timetable_scheduler.solver.*;
import com.university.timetable_scheduler.status.*;
import com.university.timetable_scheduler.tenant.TenantContext;
import com.university.timetable_scheduler.webhook.payload.ConflictMapPayload;
import lombok.AllArgsConstructor;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class TimetableServiceImpl implements TimetableService {

    // static, so @AllArgsConstructor leaves it out of the constructor
    private static final Logger log = LoggerFactory.getLogger(TimetableServiceImpl.class);

    private SectionRepository sectionRepository;
    private SectionLecturerRepository sectionLecturerRepository;
    private EventRepository eventRepository;
    private TimeslotRepository timeslotRepository;
    private RoomRepository roomRepository;
    private SectionRoomRepository sectionRoomRepository;
    private AcademicPeriodRepository academicPeriodRepository;
    private SchoolRepository schoolRepository;


    private CspModelBuilder cspModelBuilder;
    private ConflictGraphBuilder conflictGraphBuilder;
    private SolverParameters solverParameters;

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    /**
     * The academic-period id arrives as a String on the request DTOs, so a malformed value would
     * otherwise escape as IllegalArgumentException and surface as a 500 rather than a 400.
     */
    private static UUID parseAcademicPeriodId(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Academic period id, %s, is not a valid UUID".formatted(raw));
        }
    }

    /**
     * Everything a run produced, not only the part that gets persisted — the previous
     * {@code solve()} logged the rest of the {@link SolverResult} and dropped it.
     *
     * <p>{@code assignment} is ids, not entities; see {@link #applyAssignment}.
     */
    public record SolveOutcome(SolverResult result, Map<UUID, TimetableService.AssignmentIds> assignment) {}

    /**
     * Resolved on the caller's thread so a bad id is still a synchronous 400, rather than a 202
     * followed by a failure webhook.
     */
    @Transactional(readOnly = true)
    public AcademicPeriod resolvePeriod(UUID schoolId, String rawAcademicPeriodId) {
        return academicPeriodRepository
                .findByIdAndSchoolId(parseAcademicPeriodId(rawAcademicPeriodId), schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Academic period with id, %s, does not exist".formatted(rawAcademicPeriodId)));
    }

    /**
     * Re-plans the events of every section defined by duration, so a MAYBE split is decided
     * afresh on each run. Sections whose events were defined one by one are left alone.
     *
     * @return how each MAYBE section was planned this run
     */
    @Transactional
    public List<SplitDecisionDTO> prepareEvents(UUID schoolId, UUID academicPeriodId) {
        Random random = newRandom();
        List<Event> events = new ArrayList<>();
        List<SplitDecisionDTO> decisions = new ArrayList<>();
        for (Section section : sectionRepository.findSectionByFilter(schoolId, null, null, null, academicPeriodId, null)) {
            if (section.getSectionDurationMinutes() == null) continue;
            eventRepository.findEventByFilter(schoolId, null, section.getId(), null, null)
                    .forEach(e -> e.setIsDeleted(true));
            List<Event> planned = SectionEventPlanner.plan(section, random);
            if (section.getSectionSplit() == SectionEnum.SectionSplit.MAYBE && !planned.isEmpty()) {
                decisions.add(new SplitDecisionDTO(section.getId(), section.getSectionCourse().getCourseCode(),
                        section.getSectionName(), planned.size() > 1,
                        planned.stream().map(e -> e.getEventDuration().toMinutes()).toList()));
            }
            events.addAll(planned);
        }
        eventRepository.saveAll(events);
        decisions.forEach(d -> log.info("Section {} ({}) is split={} for this run", d.sectionName(),
                d.courseCode(), d.split() ? "YES" : "NO"));
        return decisions;
    }

    /**
     * Fits the school's default week to this period's events: slots are as long as the largest
     * length dividing every event's duration, capped at an hour, so every event fills a whole run
     * of slots. Default slots no longer on that grid are retired; custom ones are never touched.
     */
    @Transactional
    public void prepareTimeslots(UUID schoolId, UUID academicPeriodId) {
        int slotMinutes = 60;
        for (Event event : eventRepository.findAllBySchoolIdAndAcademicPeriodId(schoolId, academicPeriodId)) {
            if (event.getEventDuration() != null && event.getEventDuration().toMinutes() > 0) {
                slotMinutes = gcd(slotMinutes, (int) event.getEventDuration().toMinutes());
            }
        }
        slotMinutes = Math.max(slotMinutes, SectionEventPlanner.MINUTE_STEP);
        syncDefaultTimeslots(currentSchool(), slotMinutes);
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    /**
     * Pulls the whole problem into memory. Everything the solver needs must load here: the
     * transaction closes on return and the solve runs against detached entities.
     */
    @Transactional(readOnly = true)
    public Optional<CspModel> buildModel(UUID schoolId, UUID academicPeriodId) {
        return cspModelBuilder.build(schoolId, academicPeriodId);
    }

    /**
     * Deliberately <b>not</b> transactional — the point of the split. A run lasts up to
     * {@code timetable.solver.time-limit-seconds} and previously held one of five connections
     * throughout.
     *
     * <p>The model's entities are detached here. Safe only because the solver reads fields already
     * materialised and never navigates a lazy association — making any {@code Event} or
     * {@code Section} association lazy turns this into a {@code LazyInitializationException}
     * minutes into a background job.
     */
    public SolveOutcome runSolver(CspModel model) {
        SolverResult result = new AcoTimetableSolver(model, solverParameters, newRandom()).solve();

        if (!result.unschedulableEventIds().isEmpty()) {
            log.warn("{} event(s) could not be scheduled by any algorithm — their duration matches "
                            + "no contiguous block of the school's timeslots. Event ids: {}",
                    result.unschedulableEventIds().size(), result.unschedulableEventIds());
        }
        if (!result.isFeasible()) {
            log.warn("No conflict-free timetable found within the {}s budget. Best found: {}. "
                            + "Keeping it anyway so the clashes are visible.",
                    solverParameters.getTimeLimitSeconds(), result.cost());
        }

        return new SolveOutcome(result, flattenAssignment(model, result.bestSolution()));
    }

    /**
     * Seeded when {@code timetable.solver.seed} is set, so a run can be reproduced exactly;
     * otherwise fresh each time. Without a pinnable seed a disputed timetable cannot be reproduced
     * and investigated.
     */
    private Random newRandom() {
        Long seed = solverParameters.getSeed();
        return seed != null ? new Random(seed) : new Random();
    }

    /**
     * Reduces the solver's index-based solution to plain ids, in memory.
     *
     * <p>Only the block's <b>first</b> timeslot is kept — {@link Event} has one
     * {@code eventTimeslot} FK, and readers recover the extent from {@code eventDuration}. Treat it
     * as "start of block", never "the whole booking".
     */
    private Map<UUID, TimetableService.AssignmentIds> flattenAssignment(CspModel model, Solution solution) {
        Map<UUID, TimetableService.AssignmentIds> assignment = new HashMap<>();
        for (int e = 0; e < model.eventCount(); e++) {
            if (!solution.isAssigned(e)) continue;
            Candidate candidate = model.candidate(e, solution.choiceOf(e));
            assignment.put(model.event(e).getId(), new TimetableService.AssignmentIds(
                    candidate.block().timeslots().get(0).getId(),
                    candidate.room() != null ? candidate.room().getId() : null));
        }
        return assignment;
    }

    /**
     * Writes the assignment back, in a short transaction of its own.
     *
     * <p>It does <em>not</em> save the {@code Event} instances the solver held: those are minutes
     * stale, and {@code saveAll} on a detached entity merges every field of that snapshot back over
     * the row — silently reverting any edit made during the solve. Hence ids across the boundary
     * and a fresh re-read here.
     *
     * @return how many events were assigned a slot
     */
    @Transactional
    public int applyAssignment(UUID schoolId, UUID academicPeriodId,
                               Map<UUID, TimetableService.AssignmentIds> assignment) {
        if (assignment.isEmpty()) return 0;

        List<Event> events = eventRepository.findAllBySchoolIdAndAcademicPeriodId(schoolId, academicPeriodId);
        int applied = 0;
        for (Event event : events) {
            TimetableService.AssignmentIds ids = assignment.get(event.getId());
            if (ids == null) continue;
            // getReferenceById: no SELECT per timeslot and room just to set an FK.
            event.setEventTimeslot(timeslotRepository.getReferenceById(ids.timeslotId()));
            event.setEventRoom(ids.roomId() != null ? roomRepository.getReferenceById(ids.roomId()) : null);
            applied++;
        }
        // Managed entities: dirty checking flushes these, no save() needed.
        return applied;
    }


    /**
     * The persisted timetable for one period, sorted by day then start time.
     *
     * <p>Runs after {@link #applyAssignment}, so the database is the only source of truth — no
     * in-memory assignment to reconcile, and untouched events keep what they had.
     */
    @Transactional(readOnly = true)
    public TimetableResponse buildTimetableResponse(UUID schoolId, UUID academicPeriodId) {
        List<Event> events = eventRepository.findAllBySchoolIdAndAcademicPeriodId(schoolId, academicPeriodId);

        Map<UUID, String> sectionLecturerName = new HashMap<>();
        sectionLecturerRepository.findAllBySchool_Id(schoolId).forEach(sl -> {
            if (sl.getSectionLecturerSection() != null && sl.getSectionLecturerLecturer() != null) {
                sectionLecturerName.put(sl.getSectionLecturerSection().getId(),
                        sl.getSectionLecturerLecturer().displayName());
            }
        });

        Map<UUID, List<Section>> relatedByMain = relatedByMain(schoolId, academicPeriodId);

        List<TimetableEntryDTO> entries = new ArrayList<>();
        for (Event event : events) {
            if (event.getEventTimeslot() == null) continue;

            // Single-slot block: endTimeOf recovers the real extent from the duration.
            EventAssignment a = new EventAssignment(List.of(event.getEventTimeslot()), event.getEventRoom());

            TimetableEntryDTO dto = new TimetableEntryDTO();
            dto.setEventId(event.getId());
            dto.setEventType(event.getEventType() != null ? event.getEventType().name() : null);
            dto.setDurationMinutes(event.getEventDuration() != null ? event.getEventDuration().toMinutes() : 0);

            Section section = event.getEventSection();
            if (section != null) {
                List<Section> related = relatedByMain.getOrDefault(section.getId(), List.of());
                dto.setSectionName(sectionLabel(section, related));
                dto.setLecturerName(sectionLecturerName.get(section.getId()));
                if (section.getSectionCourse() != null) {
                    dto.setCourseCode(courseLabel(section, related, Course::getCourseCode));
                    dto.setCourseName(courseLabel(section, related, Course::getCourseName));
                }
            }

            Timeslot first = a.timeslots().get(0);
            dto.setDay(first.getTimeslotDay() != null ? first.getTimeslotDay().name() : null);
            dto.setStartTime(first.getTimeslotStartTime() != null ? first.getTimeslotStartTime().toString() : null);
            dto.setEndTime(endTimeOf(event, a));

            if (a.room() != null) dto.setRoom(a.room().getRoomNumber());
            entries.add(dto);
        }

        List<String> dayOrder = Arrays.asList(
                "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
        entries.sort(Comparator
                .comparingInt((TimetableEntryDTO e) ->
                        e.getDay() != null ? dayOrder.indexOf(e.getDay()) : 99)
                .thenComparing(e -> e.getStartTime() != null ? e.getStartTime() : ""));

        TimetableResponse response = new TimetableResponse();
        response.setEntries(entries);
        response.setTotalEvents(events.size());
        response.setScheduledEvents(entries.size());
        response.setUnassigned(unassignedEvents(schoolId, events, relatedByMain));
        schoolRepository.findLiveById(schoolId).ifPresent(school -> {
            if (school.getSchoolBreakStartHour() != null && school.getSchoolBreakEndHour() != null) {
                response.setBreakStartTime(school.getSchoolBreakStartHour().toString());
                response.setBreakEndTime(school.getSchoolBreakEndHour().toString());
            }
        });
        response.setResponseMessage(
                "Timetable generated. " + entries.size() + "/" + events.size() + " events scheduled.");
        return response;
    }

    /**
     * The conflict graph as a webhook payload rather than a Graphviz string. Same builder as the
     * solver and the DOT export, so all three agree; only the shape differs.
     *
     * <p>Capped at {@code webhook.max-conflict-map-sections}, with an explicit {@code truncated}
     * flag rather than a silent cut.
     */
    @Transactional(readOnly = true)
    public ConflictMapPayload buildConflictMapPayload(UUID schoolId, UUID academicPeriodId, int maxSections) {
        List<Section> sections = sectionRepository
                .findSectionByFilter(schoolId, null, null, null, academicPeriodId, null);

        boolean truncated = sections.size() > maxSections;
        if (truncated) {
            sections = sections.subList(0, maxSections);
        }

        ConflictGraphBuilder.ConflictGraphResult result = conflictGraphBuilder.build(sections);
        Graph<UUID, DefaultEdge> graph = result.graph();
        Map<UUID, Section> sectionById = result.sectionById();

        List<ConflictMapPayload.ConflictEdge> conflicts = new ArrayList<>();
        for (DefaultEdge edge : graph.edgeSet()) {
            UUID source = graph.getEdgeSource(edge);
            UUID target = graph.getEdgeTarget(edge);
            TimetableEnum.TimetableConflictReason reason =
                    result.conflictMap().get(UtilityServiceImpl.generateConflictKey(source, target));
            conflicts.add(new ConflictMapPayload.ConflictEdge(
                    source, sectionNameOf(sectionById, source),
                    target, sectionNameOf(sectionById, target),
                    reason != null ? reason.name() : null));
        }

        return new ConflictMapPayload(academicPeriodId, graph.vertexSet().size(),
                conflicts.size(), truncated, conflicts);
    }

    private static String sectionNameOf(Map<UUID, Section> sectionById, UUID id) {
        Section section = sectionById.get(id);
        return section != null ? section.getSectionName() : null;
    }

    /**
     * When an event's block ends. Prefers the block's last slot, falling back to {@code eventDuration}
     * from the start — a block rebuilt from the single persisted slot would report a 2-hour class as 1.
     */
    private static String endTimeOf(Event event, EventAssignment assignment) {
        List<Timeslot> timeslots = assignment.timeslots();
        Timeslot first = timeslots.get(0);
        if (first.getTimeslotStartTime() == null) return null;

        if (timeslots.size() > 1) {
            Timeslot last = timeslots.get(timeslots.size() - 1);
            if (last.getTimeslotEndTime() != null) return last.getTimeslotEndTime().toString();
        }
        if (event.getEventDuration() != null && !event.getEventDuration().isZero()) {
            return first.getTimeslotStartTime().plus(event.getEventDuration()).toString();
        }
        return first.getTimeslotEndTime() != null ? first.getTimeslotEndTime().toString() : null;
    }

    /** Main section id → the related sections taught with it, for "A/B" labels. */
    private Map<UUID, List<Section>> relatedByMain(UUID schoolId, UUID academicPeriodId) {
        return sectionRepository.findSectionByFilter(schoolId, null, null, null, academicPeriodId, null).stream()
                .filter(Section::isMerged)
                .collect(Collectors.groupingBy(s -> s.getSectionMergedInto().getId()));
    }

    /**
     * Each related course's value, then the main course's, joined by "/" — the order the relatedTo
     * row reads: PHY 101 related to PHY 107 is "PHY 101/PHY 107".
     */
    private static String courseLabel(Section main, List<Section> related, Function<Course, String> part) {
        return Stream.concat(related.stream(), Stream.of(main))
                .map(Section::getSectionCourse)
                .filter(Objects::nonNull)
                .map(part)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("/"));
    }

    /** The section names in the same order as {@link #courseLabel}: "PHY 101-CSC/PHY 107-CSC". */
    private static String sectionLabel(Section main, List<Section> related) {
        return Stream.concat(related.stream(), Stream.of(main))
                .map(Section::getSectionName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("/"));
    }

    /**
     * Events with no time, each with the reason. A related pair whose rooms are all too small for
     * both groups is said so; anything else could not be fitted without a clash.
     */
    private List<UnassignedEventDTO> unassignedEvents(UUID schoolId, List<Event> events,
                                                      Map<UUID, List<Section>> relatedByMain) {
        List<Event> unassigned = events.stream().filter(e -> e.getEventTimeslot() == null).toList();
        if (unassigned.isEmpty()) return List.of();

        List<Room> rooms = roomRepository.findAllBySchool_Id(schoolId);
        Map<UUID, List<Room>> roomsBySection = new HashMap<>();
        sectionRoomRepository.findAllBySchool_Id(schoolId).forEach(sr -> {
            if (sr.getSectionRoomSection() != null && sr.getSectionRoom() != null) {
                roomsBySection.computeIfAbsent(sr.getSectionRoomSection().getId(), k -> new ArrayList<>())
                        .add(sr.getSectionRoom());
            }
        });

        List<UnassignedEventDTO> result = new ArrayList<>();
        for (Event event : unassigned) {
            Section section = event.getEventSection();
            List<Section> related = section != null
                    ? relatedByMain.getOrDefault(section.getId(), List.of()) : List.of();
            String label = section == null ? null
                    : section.getSectionCourse() == null ? section.getSectionName()
                    : courseLabel(section, related, Course::getCourseCode);
            String reason = "No free time and room could be found for it without a clash";
            if (!related.isEmpty()) {
                int seats = CspModelBuilder.pairedSeats(section, related);
                List<Room> allowed = roomsBySection.getOrDefault(section.getId(), rooms);
                if (allowed.stream().noneMatch(r -> CspModelBuilder.seats(r, seats))) {
                    int largest = allowed.stream().map(Room::getRoomCapacity).filter(Objects::nonNull)
                            .mapToInt(Integer::intValue).max().orElse(0);
                    reason = "No room large enough for " + label + ": it needs " + seats + " seats and the largest "
                            + (roomsBySection.containsKey(section.getId()) ? "of its rooms" : "room") + " holds " + largest;
                }
            }
            result.add(new UnassignedEventDTO(event.getId(), label,
                    section != null ? sectionLabel(section, related) : null,
                    event.getEventDuration() != null ? event.getEventDuration().toMinutes() : 0, reason));
        }
        result.sort(Comparator.comparing(UnassignedEventDTO::courseCode, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    private static final TimeslotEnum.TimeslotDay[] WEEKDAYS = {
        TimeslotEnum.TimeslotDay.MONDAY,
        TimeslotEnum.TimeslotDay.TUESDAY,
        TimeslotEnum.TimeslotDay.WEDNESDAY,
        TimeslotEnum.TimeslotDay.THURSDAY,
        TimeslotEnum.TimeslotDay.FRIDAY
    };

    /**
     * Makes the school's default slots exactly the Mon–Fri grid of {@code slotMinutes} slots across
     * its day. Slots run from the day's start up to the break and resume when it ends; a remainder
     * shorter than a slot is left unallocated. Slots already on the grid are kept, so their ids
     * stay stable between runs.
     */
    private void syncDefaultTimeslots(School school, int slotMinutes) {
        Map<String, Timeslot> existingDefaults = new HashMap<>();
        for (Timeslot slot : timeslotRepository.findAllBySchool_Id(school.getId())) {
            if (isDefault(slot, school)) {
                existingDefaults.put(slotKey(slot.getTimeslotDay(), slot.getTimeslotStartTime(), slot.getTimeslotEndTime()), slot);
            }
        }

        List<Timeslot> toAdd = new ArrayList<>();
        for (TimeslotEnum.TimeslotDay day : WEEKDAYS) {
            LocalTime start = school.getSchoolDayStartHour();
            while (!start.plusMinutes(slotMinutes).isAfter(school.getSchoolDayEndHour())
                    && start.plusMinutes(slotMinutes).isAfter(start)) {  // guards wrapping past midnight
                LocalTime end = start.plusMinutes(slotMinutes);
                if (school.overlapsBreak(start, end)) {
                    start = school.getSchoolBreakEndHour();
                    continue;
                }
                Timeslot kept = existingDefaults.remove(slotKey(day, start, end));
                if (kept == null) {
                    toAdd.add(buildTimeslot(day, start, end, school));
                } else if (kept.getTimeslotSource() == null) {
                    kept.setTimeslotSource(TimeslotEnum.TimeslotSource.DEFAULT);
                }
                start = end;
            }
        }
        // What is left is off the grid. Soft-deleted, so events of other periods placed on them
        // still show where they were.
        existingDefaults.values().forEach(slot -> slot.setIsDeleted(true));
        timeslotRepository.saveAll(toAdd);
    }

    /**
     * Rows from before slots were tagged count as default when they match what the old seeding
     * made: a whole-hour, hour-long weekday slot within the school day.
     */
    private static boolean isDefault(Timeslot slot, School school) {
        if (slot.getTimeslotSource() != null) {
            return slot.getTimeslotSource() == TimeslotEnum.TimeslotSource.DEFAULT;
        }
        LocalTime start = slot.getTimeslotStartTime();
        LocalTime end = slot.getTimeslotEndTime();
        return start != null && end != null && slot.getTimeslotDay() != null
                && Arrays.asList(WEEKDAYS).contains(slot.getTimeslotDay())
                && start.getMinute() == 0 && end.equals(start.plusHours(1))
                && !start.isBefore(school.getSchoolDayStartHour()) && !end.isAfter(school.getSchoolDayEndHour());
    }

    private static String slotKey(TimeslotEnum.TimeslotDay day, LocalTime start, LocalTime end) {
        return day + "|" + start + "|" + end;
    }

    private Timeslot buildTimeslot(TimeslotEnum.TimeslotDay day, LocalTime start, LocalTime end, School school) {
        Timeslot ts = new Timeslot();
        ts.setSchool(school);
        ts.setTimeslotDay(day);
        ts.setTimeslotStartTime(start);
        ts.setTimeslotEndTime(end);
        ts.setTimeslotDuration(Duration.between(start, end));
        ts.setTimeslotSource(TimeslotEnum.TimeslotSource.DEFAULT);
        return ts;
    }

    /** Landscape A4 PDF of the persisted timetable, grouped by day. */
    @Override
    @Transactional(readOnly = true)
    public byte[] downloadTimetablePdf(DownloadTimetableRequest downloadTimetableRequest) {
        UUID schoolId = TenantContext.getSchoolId();

        AcademicPeriod academicPeriod = academicPeriodRepository
                .findByIdAndSchoolId(parseAcademicPeriodId(downloadTimetableRequest.getAcademicPeriodId()), schoolId)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Academic period with id, %s does not exist".formatted(downloadTimetableRequest.getAcademicPeriodId())));

        List<Event> allEvents = eventRepository.findAllBySchoolIdAndAcademicPeriodId(schoolId, academicPeriod.getId());
        List<Event> events = allEvents.stream()
                .filter(e -> e.getEventTimeslot() != null && e.getEventRoom() != null)
                .toList();

        Map<UUID, String> sectionLecturerName = new HashMap<>();
        sectionLecturerRepository.findAllBySchool_Id(schoolId).forEach(sl -> {
            if (sl.getSectionLecturerSection() != null && sl.getSectionLecturerLecturer() != null) {
                sectionLecturerName.put(sl.getSectionLecturerSection().getId(),
                        sl.getSectionLecturerLecturer().displayName());
            }
        });

        Map<UUID, List<Section>> relatedByMain = relatedByMain(schoolId, academicPeriod.getId());
        School school = currentSchool();
        LocalTime breakStart = school.getSchoolBreakStartHour();
        LocalTime breakEnd = school.getSchoolBreakEndHour();
        boolean hasBreak = breakStart != null && breakEnd != null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter   writer = new PdfWriter(baos);
            PdfDocument pdf    = new PdfDocument(writer);
            Document    doc    = new Document(pdf, PageSize.A4.rotate());
            doc.setMargins(20, 20, 20, 20);

            doc.add(new Paragraph("University Timetable")
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(12));

            Map<String, List<Event>> byDay = events.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getEventTimeslot().getTimeslotDay().name()));

            List<String> dayOrder = Arrays.asList(
                    "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");

            for (String day : dayOrder) {
                List<Event> dayEvents = byDay.get(day);
                if (dayEvents == null || dayEvents.isEmpty()) continue;

                dayEvents.sort(Comparator.comparing(
                        e -> e.getEventTimeslot().getTimeslotStartTime()));

                doc.add(new Paragraph(day)
                        .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                        .setFontSize(13)
                        .setMarginTop(14)
                        .setMarginBottom(4));

                Table table = new Table(UnitValue.createPercentArray(new float[]{14, 10, 22, 10, 18, 12, 10}))
                        .useAllAvailableWidth();

                String[] headers = {"Time", "Code", "Course", "Section", "Lecturer", "Room", "Type"};
                for (String h : headers) {
                    table.addHeaderCell(new Cell()
                            .add(new Paragraph(h)
                                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                                    .setFontSize(9))
                            .setBackgroundColor(ColorConstants.LIGHT_GRAY));
                }

                // The break goes in time order: before the first event that starts at or after it.
                boolean breakShown = !hasBreak;
                for (Event event : dayEvents) {
                    Timeslot ts      = event.getEventTimeslot();
                    if (!breakShown && !ts.getTimeslotStartTime().isBefore(breakStart)) {
                        table.addCell(breakCell(breakStart, breakEnd));
                        breakShown = true;
                    }
                    Section  section = event.getEventSection();
                    Course   course  = section != null ? section.getSectionCourse() : null;

                    String timeStr;
                    if (event.getEventDuration() != null && ts.getTimeslotStartTime() != null) {
                        LocalTime end = ts.getTimeslotStartTime().plus(event.getEventDuration());
                        timeStr = ts.getTimeslotStartTime() + " – " + end;
                    } else {
                        timeStr = ts.getTimeslotStartTime() + " – " + ts.getTimeslotEndTime();
                    }

                    table.addCell(cell(timeStr));
                    List<Section> related = section != null
                            ? relatedByMain.getOrDefault(section.getId(), List.of()) : List.of();
                    table.addCell(cell(course != null ? nullSafe(courseLabel(section, related, Course::getCourseCode)) : "-"));
                    table.addCell(cell(course != null ? nullSafe(courseLabel(section, related, Course::getCourseName)) : "-"));
                    table.addCell(cell(section != null ? nullSafe(sectionLabel(section, related)) : "-"));
                    table.addCell(cell(sectionLecturerName.getOrDefault(
                            section != null ? section.getId() : null, "-")));
                    table.addCell(cell(event.getEventRoom() != null
                            ? nullSafe(event.getEventRoom().getRoomNumber()) : "-"));
                    table.addCell(cell(event.getEventType() != null
                            ? event.getEventType().name() : "-"));
                }
                if (!breakShown) table.addCell(breakCell(breakStart, breakEnd));

                doc.add(table);
            }

            List<UnassignedEventDTO> unassigned = unassignedEvents(schoolId, allEvents, relatedByMain);
            if (!unassigned.isEmpty()) {
                doc.add(new Paragraph("UNASSIGNED")
                        .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                        .setFontSize(13)
                        .setMarginTop(14)
                        .setMarginBottom(4));
                for (int i = 0; i < unassigned.size(); i++) {
                    UnassignedEventDTO u = unassigned.get(i);
                    doc.add(new Paragraph((i + 1) + ". " + nullSafe(u.courseCode()) + " (" + nullSafe(u.sectionName())
                            + ", " + u.durationMinutes() + " min): " + u.reason()).setFontSize(9));
                }
            }

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate timetable PDF: " + e.getMessage(), e);
        }
    }

    private static Cell cell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(8));
    }
    /** A full-width row marking the daily break. */
    private static Cell breakCell(LocalTime start, LocalTime end) {
        return new Cell(1, 7)
                .add(new Paragraph(start + " – " + end + "   BREAK").setFontSize(8))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER);
    }
    private static String nullSafe(String s) { return s != null ? s : "-"; }

    /**
     * Graphviz DOT of the conflict graph — red = same lecturer, orange = overlapping students.
     * Paste at <a href="https://dreampuf.github.io/GraphvizOnline/">GraphvizOnline</a>.
     */
    @Override
    public String getConflictGraphDot(DownloadConflictGraphRequest downloadConflictGraphRequest) {
        UUID schoolId = TenantContext.getSchoolId();
        AcademicPeriod academicPeriod = academicPeriodRepository
                .findByIdAndSchoolId(parseAcademicPeriodId(downloadConflictGraphRequest.getAcademicPeriodId()), schoolId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST, "Academic period with id %s does not exist".formatted(downloadConflictGraphRequest.getAcademicPeriodId())));
        List<Section> sections = sectionRepository
                .findSectionByFilter(schoolId, null, null,null, academicPeriod.getId(), null);

        // Same builder the solver uses, so the picture matches what was solved.
        ConflictGraphBuilder.ConflictGraphResult result = conflictGraphBuilder.build(sections);
        Graph<UUID, DefaultEdge> graph = result.graph();
        Map<String, TimetableEnum.TimetableConflictReason> conflictMap = result.conflictMap();
        Map<UUID, Section> sectionById = result.sectionById();

        StringBuilder sb = new StringBuilder();
        sb.append("graph ConflictGraph {\n");
        sb.append("  rankdir=LR;\n");
        sb.append("  node [shape=ellipse, style=filled, fillcolor=lightblue];\n\n");

        // Nodes
        for (UUID id : graph.vertexSet()) {
            Section s = sectionById.get(id);
            String label = (s != null && s.getSectionName() != null)
                    ? s.getSectionName().replace("\"", "'")
                    : id.toString();
            sb.append("  \"").append(id).append("\" [label=\"").append(label).append("\"];\n");
        }

        sb.append("\n");

        // Edges
        for (DefaultEdge edge : graph.edgeSet()) {
            UUID src = graph.getEdgeSource(edge);
            UUID tgt = graph.getEdgeTarget(edge);
            String key = UtilityServiceImpl.generateConflictKey(src, tgt);
            TimetableEnum.TimetableConflictReason reason = conflictMap.get(key);

            String color = "black";
            String label = "";
            if (reason == TimetableEnum.TimetableConflictReason.SAME_LECTURER) {
                color = "red";
                label = "SAME_LECTURER";
            } else if (reason == TimetableEnum.TimetableConflictReason.OVERLAPPING_STUDENTS) {
                color = "orange";
                label = "OVERLAPPING_STUDENTS";
            }

            sb.append("  \"").append(src).append("\" -- \"").append(tgt)
              .append("\" [label=\"").append(label).append("\", color=").append(color).append("];\n");
        }

        sb.append("}\n");
        return sb.toString();
    }
}
