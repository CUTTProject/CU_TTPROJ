package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.timetable.ReadTimetableConflictRequest;
import com.university.timetable_scheduler.dto.response.timetable.ReadTimetableConflictResponse;
import com.university.timetable_scheduler.dto.response.timetable.TimetableConflictDTO;
import com.university.timetable_scheduler.dto.response.timetable.TimetableGenerationDTO;
import com.university.timetable_scheduler.dto.response.timetable.UnassignedEventDTO;
import com.university.timetable_scheduler.entity.*;
import com.university.timetable_scheduler.generation.GenerationJob;
import com.university.timetable_scheduler.repository.*;
import com.university.timetable_scheduler.solver.ConflictCounter;
import com.university.timetable_scheduler.solver.ConflictGraphBuilder;
import com.university.timetable_scheduler.solver.SolverResult;
import com.university.timetable_scheduler.status.JobEnum;
import com.university.timetable_scheduler.status.TimetableEnum.TimetableConflictReason;
import com.university.timetable_scheduler.status.TimetableEnum.TimetableConflictType;
import com.university.timetable_scheduler.tenant.TenantContext;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Records what each generation left wrong with its timetable, and reads back the latest.
 *
 * <p>{@link #recordSuccess} applies the solver's assignment and writes the conflicts in one
 * transaction, so the saved timetable and its conflict list can never disagree.
 */
@Service
@AllArgsConstructor
public class TimetableConflictServiceImpl {

    private static final List<String> DAY_ORDER = List.of(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");

    private final TimetableServiceImpl timetableService;
    private final TimetableGenerationRepository generationRepository;
    private final TimetableConflictRepository conflictRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final SchoolRepository schoolRepository;
    private final EventRepository eventRepository;
    private final SectionRepository sectionRepository;
    private final SectionLecturerRepository sectionLecturerRepository;
    private final ConflictGraphBuilder conflictGraphBuilder;
    private final Clock clock;

    // ─────────────────────────────────────────────────────────────────────────────
    // Recording
    // ─────────────────────────────────────────────────────────────────────────────

    /** Applies the assignment and records the run and every conflict it left. */
    @Transactional
    public TimetableGeneration recordSuccess(GenerationJob job, TimetableServiceImpl.SolveOutcome outcome) {
        UUID schoolId = job.getSchoolId();
        UUID periodId = job.getAcademicPeriodId();
        SolverResult result = outcome.result();
        ConflictCounter.CostBreakdown breakdown =
                result.breakdown() != null ? result.breakdown() : ConflictCounter.CostBreakdown.none();

        Set<UUID> unplaced = new HashSet<>(breakdown.unassignedEventIds());
        unplaced.addAll(result.unschedulableEventIds());
        timetableService.applyAssignment(schoolId, periodId, outcome.assignment(), unplaced);

        List<Event> events = eventRepository.findAllBySchoolIdAndAcademicPeriodId(schoolId, periodId);
        Map<UUID, Event> eventById = events.stream().collect(Collectors.toMap(Event::getId, Function.identity()));
        Map<UUID, List<Section>> relatedByMain = timetableService.relatedByMain(schoolId, periodId);

        TimetableGeneration generation = newGeneration(job, JobEnum.JobStatus.SUCCEEDED);
        generation.setFeasible(result.isFeasible());
        generation.setStopReason(result.stoppedBecause() != null ? result.stoppedBecause().name() : null);
        generation.setTotalEvents(events.size());
        generation.setScheduledEvents((int) events.stream().filter(e -> e.getEventTimeslot() != null).count());

        List<TimetableConflict> conflicts = new ArrayList<>();
        conflicts.addAll(clashRows(schoolId, periodId, breakdown.clashes(), eventById, relatedByMain));
        conflicts.addAll(unplacedRows(schoolId, breakdown.unassignedEventIds(), TimetableConflictType.UNASSIGNED,
                eventById, relatedByMain));
        conflicts.addAll(unplacedRows(schoolId, result.unschedulableEventIds(), TimetableConflictType.UNSCHEDULABLE,
                eventById, relatedByMain));

        Map<TimetableConflictType, Long> counts = conflicts.stream()
                .collect(Collectors.groupingBy(TimetableConflict::getConflictType, Collectors.counting()));
        generation.setRoomClashes(count(counts, TimetableConflictType.ROOM_CLASH));
        generation.setLecturerClashes(count(counts, TimetableConflictType.LECTURER_CLASH));
        generation.setStudentClashes(count(counts, TimetableConflictType.STUDENT_CLASH));
        generation.setUnassignedEvents(count(counts, TimetableConflictType.UNASSIGNED));
        generation.setUnschedulableEvents(count(counts, TimetableConflictType.UNSCHEDULABLE));

        TimetableGeneration saved = generationRepository.save(generation);
        conflicts.forEach(c -> {
            c.setGeneration(saved);
            c.setSchool(saved.getSchool());
        });
        conflictRepository.saveAll(conflicts);
        return saved;
    }

    /** Records a run that produced no timetable. Nothing on the Event rows changed because of it. */
    @Transactional
    public void recordFailure(GenerationJob job, String message) {
        TimetableGeneration generation = newGeneration(job, JobEnum.JobStatus.FAILED);
        generation.setFailureMessage(message);
        generationRepository.save(generation);
    }

    private TimetableGeneration newGeneration(GenerationJob job, JobEnum.JobStatus status) {
        TimetableGeneration generation = new TimetableGeneration();
        generation.setSchool(schoolRepository.getReferenceById(job.getSchoolId()));
        generation.setAcademicPeriod(academicPeriodRepository.getReferenceById(job.getAcademicPeriodId()));
        generation.setJobId(job.getJobId());
        generation.setGenerationStatus(status);
        generation.setSubmittedAt(job.getSubmittedAt());
        generation.setStartedAt(job.getStartedAt());
        generation.setFinishedAt(Instant.now(clock));
        return generation;
    }

    private static int count(Map<TimetableConflictType, Long> counts, TimetableConflictType type) {
        return counts.getOrDefault(type, 0L).intValue();
    }

    /**
     * One row per clashing pair. The solver only says whether the pair shares a room; for the rest
     * the conflict graph says whether they share a lecturer or students, checked across each
     * event's related sections too, since a main section's events carry its related sections'
     * constraints.
     */
    private List<TimetableConflict> clashRows(UUID schoolId, UUID periodId, List<ConflictCounter.Clash> clashes,
                                              Map<UUID, Event> eventById, Map<UUID, List<Section>> relatedByMain) {
        if (clashes.isEmpty()) return List.of();

        boolean needsGraph = clashes.stream().anyMatch(c -> !c.sameRoom());
        Map<String, TimetableConflictReason> reasons = needsGraph
                ? conflictGraphBuilder.build(sectionRepository.findSectionByFilter(schoolId, null, null, null, periodId, null))
                        .conflictMap()
                : Map.of();
        Map<UUID, Set<String>> lecturersBySection = needsGraph ? lecturersBySection(schoolId) : Map.of();

        List<TimetableConflict> rows = new ArrayList<>();
        for (ConflictCounter.Clash clash : clashes) {
            Event a = eventById.get(clash.eventA());
            Event b = eventById.get(clash.eventB());
            if (a == null || b == null) continue;

            TimetableConflict row = new TimetableConflict();
            fillEventA(row, a, relatedByMain);
            fillEventB(row, b, relatedByMain);
            row.setConflictDay(dayOf(a));

            if (clash.sameRoom()) {
                row.setConflictType(TimetableConflictType.ROOM_CLASH);
                row.setConflictReason("Both are booked into " + nullSafe(row.getEventARoom()) + " at overlapping times");
            } else {
                classifyTimeClash(row, a, b, reasons, lecturersBySection, relatedByMain);
            }
            rows.add(row);
        }
        return rows;
    }

    private void classifyTimeClash(TimetableConflict row, Event a, Event b,
                                   Map<String, TimetableConflictReason> reasons,
                                   Map<UUID, Set<String>> lecturersBySection,
                                   Map<UUID, List<Section>> relatedByMain) {
        Section sectionA = a.getEventSection();
        Section sectionB = b.getEventSection();
        if (sectionA != null && sectionB != null && sectionA.getId().equals(sectionB.getId())) {
            row.setConflictType(TimetableConflictType.LECTURER_CLASH);
            row.setConflictReason("Two sessions of " + nullSafe(row.getEventASectionName())
                    + " overlap, so its lecturer and students are booked twice");
            return;
        }

        Set<UUID> groupA = group(sectionA, relatedByMain);
        Set<UUID> groupB = group(sectionB, relatedByMain);
        boolean sharesLecturer = false;
        boolean sharesStudents = false;
        for (UUID x : groupA) {
            for (UUID y : groupB) {
                TimetableConflictReason reason = reasons.get(UtilityServiceImpl.generateConflictKey(x, y));
                if (reason == TimetableConflictReason.SAME_LECTURER) sharesLecturer = true;
                else if (reason == TimetableConflictReason.OVERLAPPING_STUDENTS) sharesStudents = true;
            }
        }

        if (sharesLecturer) {
            Set<String> lecturersA = names(groupA, lecturersBySection);
            Set<String> shared = new TreeSet<>(names(groupB, lecturersBySection));
            shared.retainAll(lecturersA);
            row.setConflictType(TimetableConflictType.LECTURER_CLASH);
            row.setConflictReason(shared.isEmpty()
                    ? "Both are taught by the same lecturer at overlapping times"
                    : "Both are taught by " + String.join(", ", shared) + " at overlapping times");
        } else if (sharesStudents) {
            row.setConflictType(TimetableConflictType.STUDENT_CLASH);
            row.setConflictReason("Some students are enrolled in both, and they overlap in time");
        } else {
            // The solver only links events whose sections share a lecturer or students, so this
            // is reached only if the data changed between building the model and recording.
            row.setConflictType(TimetableConflictType.STUDENT_CLASH);
            row.setConflictReason("They share a lecturer or students, and they overlap in time");
        }
    }

    private static Set<UUID> group(Section section, Map<UUID, List<Section>> relatedByMain) {
        if (section == null) return Set.of();
        Set<UUID> ids = new HashSet<>();
        ids.add(section.getId());
        relatedByMain.getOrDefault(section.getId(), List.of()).forEach(s -> ids.add(s.getId()));
        return ids;
    }

    private static Set<String> names(Set<UUID> sectionIds, Map<UUID, Set<String>> lecturersBySection) {
        Set<String> names = new HashSet<>();
        sectionIds.forEach(id -> names.addAll(lecturersBySection.getOrDefault(id, Set.of())));
        return names;
    }

    private Map<UUID, Set<String>> lecturersBySection(UUID schoolId) {
        Map<UUID, Set<String>> bySection = new HashMap<>();
        sectionLecturerRepository.findAllBySchool_Id(schoolId).forEach(sl -> {
            if (sl.getSectionLecturerSection() != null && sl.getSectionLecturerLecturer() != null) {
                bySection.computeIfAbsent(sl.getSectionLecturerSection().getId(), k -> new HashSet<>())
                        .add(sl.getSectionLecturerLecturer().displayName());
            }
        });
        return bySection;
    }

    /** One row per event that got no time, with the reason the timetable PDF gives for it. */
    private List<TimetableConflict> unplacedRows(UUID schoolId, Collection<UUID> eventIds, TimetableConflictType type,
                                                 Map<UUID, Event> eventById, Map<UUID, List<Section>> relatedByMain) {
        List<Event> unplaced = eventIds.stream().map(eventById::get).filter(Objects::nonNull).toList();
        Map<UUID, String> reasons = timetableService.describeUnplaced(schoolId, unplaced, relatedByMain).stream()
                .collect(Collectors.toMap(UnassignedEventDTO::eventId, UnassignedEventDTO::reason, (x, y) -> x));

        List<TimetableConflict> rows = new ArrayList<>();
        for (Event event : unplaced) {
            TimetableConflict row = new TimetableConflict();
            row.setConflictType(type);
            fillEventA(row, event, relatedByMain);

            String reason = reasons.getOrDefault(event.getId(), TimetableServiceImpl.NO_CLASH_FREE_PLACE);
            if (type == TimetableConflictType.UNSCHEDULABLE && reason.equals(TimetableServiceImpl.NO_CLASH_FREE_PLACE)) {
                long minutes = event.getEventDuration() != null ? event.getEventDuration().toMinutes() : 0;
                reason = "Its length of " + minutes + " minutes matches no run of back-to-back timeslots";
            }
            row.setConflictReason(reason);
            rows.add(row);
        }
        return rows;
    }

    private void fillEventA(TimetableConflict row, Event event, Map<UUID, List<Section>> relatedByMain) {
        row.setEventAId(event.getId());
        row.setEventACourseCode(courseCodeOf(event, relatedByMain));
        row.setEventASectionName(sectionNameOf(event, relatedByMain));
        row.setEventAStartTime(startOf(event));
        row.setEventAEndTime(endOf(event));
        row.setEventARoom(roomOf(event));
    }

    private void fillEventB(TimetableConflict row, Event event, Map<UUID, List<Section>> relatedByMain) {
        row.setEventBId(event.getId());
        row.setEventBCourseCode(courseCodeOf(event, relatedByMain));
        row.setEventBSectionName(sectionNameOf(event, relatedByMain));
        row.setEventBStartTime(startOf(event));
        row.setEventBEndTime(endOf(event));
        row.setEventBRoom(roomOf(event));
    }

    private static String courseCodeOf(Event event, Map<UUID, List<Section>> relatedByMain) {
        Section section = event.getEventSection();
        if (section == null || section.getSectionCourse() == null) return null;
        return TimetableServiceImpl.courseLabel(section, relatedByMain.getOrDefault(section.getId(), List.of()),
                Course::getCourseCode);
    }

    private static String sectionNameOf(Event event, Map<UUID, List<Section>> relatedByMain) {
        Section section = event.getEventSection();
        if (section == null) return null;
        return TimetableServiceImpl.sectionLabel(section, relatedByMain.getOrDefault(section.getId(), List.of()));
    }

    private static String dayOf(Event event) {
        Timeslot slot = event.getEventTimeslot();
        return slot != null && slot.getTimeslotDay() != null ? slot.getTimeslotDay().name() : null;
    }

    private static String startOf(Event event) {
        Timeslot slot = event.getEventTimeslot();
        return slot != null && slot.getTimeslotStartTime() != null ? slot.getTimeslotStartTime().toString() : null;
    }

    /** Only the block's first slot is stored on the event, so the end comes from the duration. */
    private static String endOf(Event event) {
        Timeslot slot = event.getEventTimeslot();
        if (slot == null || slot.getTimeslotStartTime() == null) return null;
        if (event.getEventDuration() != null && !event.getEventDuration().isZero()) {
            return slot.getTimeslotStartTime().plus(event.getEventDuration()).toString();
        }
        LocalTime end = slot.getTimeslotEndTime();
        return end != null ? end.toString() : null;
    }

    private static String roomOf(Event event) {
        return event.getEventRoom() != null ? event.getEventRoom().getRoomNumber() : null;
    }

    private static String nullSafe(String s) {
        return s != null ? s : "-";
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Reading
    // ─────────────────────────────────────────────────────────────────────────────

    /** The conflicts of the latest successful generation for a period. 404 when there is none yet. */
    @Transactional(readOnly = true)
    public ReadTimetableConflictResponse readLatestConflicts(ReadTimetableConflictRequest request) {
        UUID schoolId = TenantContext.getSchoolId();
        AcademicPeriod period = timetableService.resolvePeriod(schoolId, request.getAcademicPeriodId());

        TimetableGeneration generation = latestSuccessful(schoolId, period.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No timetable has been generated for academic period '%s' yet."
                                .formatted(period.getAcademicPeriodName())));

        ReadTimetableConflictResponse.Data data = new ReadTimetableConflictResponse.Data();
        data.setGeneration(toDto(generation, period.getId()));
        data.setConflicts(conflictsOf(schoolId, generation).stream().map(TimetableConflictServiceImpl::toDto).toList());

        ReadTimetableConflictResponse response = new ReadTimetableConflictResponse();
        response.setData(data);
        response.setResponseMessage(data.getConflicts().isEmpty()
                ? "The latest timetable has no conflicts."
                : "The latest timetable has " + data.getConflicts().size() + " conflict(s).");
        return response;
    }

    public Optional<TimetableGeneration> latestSuccessful(UUID schoolId, UUID academicPeriodId) {
        return generationRepository
                .findFirstBySchool_IdAndAcademicPeriod_IdAndGenerationStatusAndIsDeletedFalseOrderByFinishedAtDesc(
                        schoolId, academicPeriodId, JobEnum.JobStatus.SUCCEEDED);
    }

    /** A generation's conflicts in reading order: by type, then day, then start time. */
    public List<TimetableConflict> conflictsOf(UUID schoolId, TimetableGeneration generation) {
        List<TimetableConflict> conflicts = new ArrayList<>(
                conflictRepository.findAllBySchool_IdAndGeneration_IdAndIsDeletedFalse(schoolId, generation.getId()));
        conflicts.sort(Comparator
                .comparing(TimetableConflict::getConflictType, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(c -> c.getConflictDay() != null ? DAY_ORDER.indexOf(c.getConflictDay()) : 99)
                .thenComparing(c -> c.getEventAStartTime() != null ? c.getEventAStartTime() : "")
                .thenComparing(c -> c.getEventACourseCode() != null ? c.getEventACourseCode() : ""));
        return conflicts;
    }

    /** A finished run by job id, for when the in-memory job record is gone after a restart. */
    @Transactional(readOnly = true)
    public Optional<TimetableGeneration> findByJobId(UUID schoolId, UUID jobId) {
        return generationRepository.findFirstBySchool_IdAndJobIdAndIsDeletedFalse(schoolId, jobId);
    }

    public static TimetableGenerationDTO toDto(TimetableGeneration g, UUID academicPeriodId) {
        return new TimetableGenerationDTO(
                g.getId(), g.getJobId(), academicPeriodId,
                g.getGenerationStatus() != null ? g.getGenerationStatus().name() : null,
                g.getStartedAt() != null ? g.getStartedAt().toString() : null,
                g.getFinishedAt() != null ? g.getFinishedAt().toString() : null,
                g.getFeasible(), g.getStopReason(), g.getTotalEvents(), g.getScheduledEvents(),
                g.getRoomClashes(), g.getLecturerClashes(), g.getStudentClashes(),
                g.getUnassignedEvents(), g.getUnschedulableEvents());
    }

    private static TimetableConflictDTO toDto(TimetableConflict c) {
        TimetableConflictDTO.ConflictEventDTO a = new TimetableConflictDTO.ConflictEventDTO(
                c.getEventAId(), c.getEventACourseCode(), c.getEventASectionName(),
                c.getEventAStartTime(), c.getEventAEndTime(), c.getEventARoom());
        TimetableConflictDTO.ConflictEventDTO b = c.getEventBId() == null ? null
                : new TimetableConflictDTO.ConflictEventDTO(
                        c.getEventBId(), c.getEventBCourseCode(), c.getEventBSectionName(),
                        c.getEventBStartTime(), c.getEventBEndTime(), c.getEventBRoom());
        return new TimetableConflictDTO(c.getId(),
                c.getConflictType() != null ? c.getConflictType().name() : null,
                c.getConflictDay(), c.getConflictReason(), a, b);
    }
}
