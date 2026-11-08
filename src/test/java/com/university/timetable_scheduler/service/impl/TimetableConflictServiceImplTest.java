package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.timetable.ReadTimetableConflictRequest;
import com.university.timetable_scheduler.dto.response.timetable.ReadTimetableConflictResponse;
import com.university.timetable_scheduler.dto.response.timetable.TimetableConflictDTO;
import com.university.timetable_scheduler.entity.*;
import com.university.timetable_scheduler.generation.GenerationJob;
import com.university.timetable_scheduler.repository.*;
import com.university.timetable_scheduler.service.TimetableService;
import com.university.timetable_scheduler.solver.ConflictCounter;
import com.university.timetable_scheduler.solver.SolutionCost;
import com.university.timetable_scheduler.solver.SolverResult;
import com.university.timetable_scheduler.status.TimeslotEnum;
import com.university.timetable_scheduler.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A generation's conflicts are recorded with the assignment and read back from the latest run.
 * The solver is not run: its outcome is built by hand so each kind of conflict appears exactly once.
 */
@SpringBootTest
class TimetableConflictServiceImplTest {

    @Autowired private TimetableConflictServiceImpl conflictService;
    @Autowired private TimetableServiceImpl timetableService;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private AcademicPeriodRepository academicPeriodRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private SectionRepository sectionRepository;
    @Autowired private LecturerRepository lecturerRepository;
    @Autowired private SectionLecturerRepository sectionLecturerRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private TimeslotRepository timeslotRepository;
    @Autowired private EventRepository eventRepository;

    private School school;
    private AcademicPeriod period;
    private Timeslot monday9;
    private Room r1;
    private Room r2;
    private Event e1, e2, e3, e4, e5;

    @BeforeEach
    void setUp() {
        School s = new School();
        String name = "school-" + UUID.randomUUID();
        s.setSchoolName(name);
        s.setSchoolAdminEmail("admin@" + name + ".test");
        s.setSchoolAdminPassword("irrelevant-hash");
        s.setSchoolAddress(name + " address");
        s.setSchoolPhone("phone-" + name);
        s.setSchoolDayStartHour(LocalTime.of(8, 0));
        s.setSchoolDayEndHour(LocalTime.of(18, 0));
        school = schoolRepository.save(s);
        TenantContext.setSchoolId(school.getId());

        AcademicPeriod p = new AcademicPeriod();
        p.setSchool(school);
        p.setAcademicPeriodName("2026 First");
        p.setAcademicPeriodStartDate(LocalDateTime.now().minusDays(10));
        p.setAcademicPeriodEndDate(LocalDateTime.now().plusDays(100));
        period = academicPeriodRepository.save(p);

        Lecturer shared = lecturer("STF1", "Ada", "Lovelace");
        Lecturer other = lecturer("STF2", "Alan", "Turing");

        Section s1 = section("CSC101", "CSC101-A", shared);
        Section s2 = section("CSC102", "CSC102-A", shared);
        Section s3 = section("CSC103", "CSC103-A", other);
        Section s4 = section("CSC104", "CSC104-A", other);

        r1 = room("R1");
        r2 = room("R2");

        Timeslot t = new Timeslot();
        t.setSchool(school);
        t.setTimeslotDay(TimeslotEnum.TimeslotDay.MONDAY);
        t.setTimeslotStartTime(LocalTime.of(9, 0));
        t.setTimeslotEndTime(LocalTime.of(10, 0));
        t.setTimeslotDuration(Duration.ofHours(1));
        monday9 = timeslotRepository.save(t);

        e1 = event(s1, 60);
        e2 = event(s2, 60);
        e3 = event(s3, 60);
        e4 = event(s3, 60);
        e5 = event(s4, 90);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Lecturer lecturer(String staff, String first, String last) {
        Lecturer l = new Lecturer();
        l.setSchool(school);
        l.setLecturerStaffNumber(staff);
        l.setLecturerFirstName(first);
        l.setLecturerLastName(last);
        return lecturerRepository.save(l);
    }

    private Section section(String code, String sectionName, Lecturer lecturer) {
        Course c = new Course();
        c.setSchool(school);
        c.setCourseCode(code);
        c.setCourseName(code + " name");
        c = courseRepository.save(c);

        Section s = new Section();
        s.setSchool(school);
        s.setSectionCourse(c);
        s.setSectionName(sectionName);
        s.setSectionAcademicPeriod(period);
        s = sectionRepository.save(s);

        SectionLecturer sl = new SectionLecturer();
        sl.setSchool(school);
        sl.setSectionLecturerSection(s);
        sl.setSectionLecturerLecturer(lecturer);
        sectionLecturerRepository.save(sl);
        return s;
    }

    private Room room(String number) {
        Room r = new Room();
        r.setSchool(school);
        r.setRoomNumber(number);
        r.setRoomCapacity(100);
        return roomRepository.save(r);
    }

    private Event event(Section section, int minutes) {
        Event e = new Event();
        e.setSchool(school);
        e.setEventSection(section);
        e.setEventDuration(Duration.ofMinutes(minutes));
        return eventRepository.save(e);
    }

    /**
     * e1 and e3 share R1 at 9:00 (room clash); e1 and e2 share a lecturer at 9:00 in different
     * rooms (lecturer clash); e4 was left unplaced; e5 could never be placed.
     */
    private TimetableServiceImpl.SolveOutcome outcome() {
        Map<UUID, TimetableService.AssignmentIds> assignment = Map.of(
                e1.getId(), new TimetableService.AssignmentIds(monday9.getId(), r1.getId()),
                e2.getId(), new TimetableService.AssignmentIds(monday9.getId(), r2.getId()),
                e3.getId(), new TimetableService.AssignmentIds(monday9.getId(), r1.getId()));
        ConflictCounter.CostBreakdown breakdown = new ConflictCounter.CostBreakdown(
                1, 1, 1, Set.of(e1.getId(), e2.getId(), e3.getId(), e4.getId()),
                Set.of(e4.getId()),
                List.of(new ConflictCounter.Clash(e1.getId(), e2.getId(), false),
                        new ConflictCounter.Clash(e1.getId(), e3.getId(), true)));
        SolverResult result = new SolverResult(null, new SolutionCost(1, 2), breakdown, 1, Duration.ofSeconds(1),
                SolverResult.StopReason.TIME_LIMIT_REACHED, List.of(e5.getId()));
        return new TimetableServiceImpl.SolveOutcome(result, assignment);
    }

    private GenerationJob job() {
        GenerationJob job = new GenerationJob(school.getId(), period.getId(), Instant.now());
        job.markRunning(Instant.now());
        return job;
    }

    private ReadTimetableConflictResponse read() {
        return conflictService.readLatestConflicts(new ReadTimetableConflictRequest(period.getId().toString()));
    }

    @Test
    @DisplayName("Each kind of conflict is recorded once, with a reason, and counted on the generation")
    void recordsEveryKind() {
        GenerationJob job = job();
        conflictService.recordSuccess(job, outcome());

        ReadTimetableConflictResponse.Data data = read().getData();
        assertThat(data.getGeneration().jobId()).isEqualTo(job.getJobId());
        assertThat(data.getGeneration().feasible()).isFalse();
        assertThat(data.getGeneration().totalEvents()).isEqualTo(5);
        assertThat(data.getGeneration().scheduledEvents()).isEqualTo(3);
        assertThat(data.getGeneration().roomClashes()).isEqualTo(1);
        assertThat(data.getGeneration().lecturerClashes()).isEqualTo(1);
        assertThat(data.getGeneration().studentClashes()).isZero();
        assertThat(data.getGeneration().unassignedEvents()).isEqualTo(1);
        assertThat(data.getGeneration().unschedulableEvents()).isEqualTo(1);

        assertThat(data.getConflicts()).extracting(TimetableConflictDTO::conflictType)
                .containsExactly("ROOM_CLASH", "LECTURER_CLASH", "UNASSIGNED", "UNSCHEDULABLE");

        TimetableConflictDTO room = data.getConflicts().get(0);
        assertThat(room.day()).isEqualTo("MONDAY");
        assertThat(room.eventA().room()).isEqualTo("R1");
        assertThat(room.eventB().room()).isEqualTo("R1");
        assertThat(room.reason()).contains("R1");

        TimetableConflictDTO lecturer = data.getConflicts().get(1);
        assertThat(lecturer.reason()).contains("Ada Lovelace");
        assertThat(lecturer.eventA().startTime()).isEqualTo("09:00");
        assertThat(lecturer.eventA().endTime()).isEqualTo("10:00");

        TimetableConflictDTO unassigned = data.getConflicts().get(2);
        assertThat(unassigned.eventB()).isNull();
        assertThat(unassigned.eventA().courseCode()).isEqualTo("CSC103");

        assertThat(data.getConflicts().get(3).reason()).contains("90 minutes");
    }

    @Test
    @DisplayName("An event left unplaced loses the time and room an earlier run gave it")
    void unplacedEventLosesOldSlot() {
        // As if an earlier run had placed e4 and e5.
        e4.setEventTimeslot(monday9);
        e4.setEventRoom(r2);
        e5.setEventTimeslot(monday9);
        e5.setEventRoom(r2);
        eventRepository.saveAll(List.of(e4, e5));
        Event untouched = event(sectionRepository.findById(e1.getEventSection().getId()).orElseThrow(), 60);
        untouched.setEventTimeslot(monday9);
        untouched.setEventRoom(r2);
        eventRepository.save(untouched);

        conflictService.recordSuccess(job(), outcome());

        assertThat(eventRepository.findById(e4.getId()).orElseThrow().getEventTimeslot()).isNull();
        assertThat(eventRepository.findById(e4.getId()).orElseThrow().getEventRoom()).isNull();
        assertThat(eventRepository.findById(e5.getId()).orElseThrow().getEventTimeslot()).isNull();
        // Not in this run's model at all (added mid-solve), so it keeps its place.
        assertThat(eventRepository.findById(untouched.getId()).orElseThrow().getEventTimeslot()).isNotNull();

        TimetableConflictDTO unassigned = read().getData().getConflicts().stream()
                .filter(c -> c.conflictType().equals("UNASSIGNED")).findFirst().orElseThrow();
        assertThat(unassigned.eventA().startTime()).isNull();
        assertThat(unassigned.eventA().room()).isNull();
    }

    @Test
    @DisplayName("The latest successful run is returned; a later failure does not hide it")
    void latestSuccessfulWins() {
        conflictService.recordSuccess(job(), outcome());

        GenerationJob clean = job();
        SolverResult feasible = new SolverResult(null, new SolutionCost(0, 0), ConflictCounter.CostBreakdown.none(),
                1, Duration.ofSeconds(1), SolverResult.StopReason.FEASIBLE_SOLUTION_FOUND, List.of());
        conflictService.recordSuccess(clean, new TimetableServiceImpl.SolveOutcome(feasible, Map.of()));
        conflictService.recordFailure(job(), "The generation failed unexpectedly.");

        ReadTimetableConflictResponse.Data data = read().getData();
        assertThat(data.getGeneration().jobId()).isEqualTo(clean.getJobId());
        assertThat(data.getGeneration().feasible()).isTrue();
        assertThat(data.getConflicts()).isEmpty();
    }

    @Test
    @DisplayName("A failed run is found by its job id")
    void failureFoundByJobId() {
        GenerationJob failed = job();
        conflictService.recordFailure(failed, "No events, rooms or timeslots were found for this academic period.");

        assertThat(conflictService.findByJobId(school.getId(), failed.getJobId()))
                .hasValueSatisfying(g -> assertThat(g.getFailureMessage()).startsWith("No events"));
    }

    @Test
    @DisplayName("The timetable PDF still renders once clashes are recorded")
    void pdfRendersWithConflicts() {
        conflictService.recordSuccess(job(), outcome());
        byte[] pdf = timetableService.downloadTimetablePdf(
                new com.university.timetable_scheduler.dto.request.timetable.DownloadTimetableRequest(period.getId().toString()));
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("404 before the first generation")
    void notFoundBeforeFirstRun() {
        assertThatThrownBy(this::read)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
