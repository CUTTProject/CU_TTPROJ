package com.university.timetable_scheduler.bulk;

import com.university.timetable_scheduler.dto.request.section.CreateSectionRequest;
import com.university.timetable_scheduler.dto.request.section.UpdateSectionRequest;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.timetable.SplitDecisionDTO;
import com.university.timetable_scheduler.entity.AcademicPeriod;
import com.university.timetable_scheduler.entity.Course;
import com.university.timetable_scheduler.entity.Department;
import com.university.timetable_scheduler.entity.Enrollment;
import com.university.timetable_scheduler.entity.Event;
import com.university.timetable_scheduler.entity.Program;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.entity.Section;
import com.university.timetable_scheduler.entity.SectionLecturer;
import com.university.timetable_scheduler.entity.Student;
import com.university.timetable_scheduler.entity.Timeslot;
import com.university.timetable_scheduler.mapper.StudentMapper;
import com.university.timetable_scheduler.repository.*;
import com.university.timetable_scheduler.service.impl.*;
import com.university.timetable_scheduler.solver.CspModel;
import com.university.timetable_scheduler.status.CourseEnum;
import com.university.timetable_scheduler.status.SectionEnum;
import com.university.timetable_scheduler.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The uploads end to end against H2, in the order a school runs them. Deliberately not
 * {@code @Transactional}: a dry run works by rolling back the service's own transaction, which a
 * surrounding test transaction would swallow.
 */
@SpringBootTest
class BulkUploadFlowTest {

    @Autowired private DepartmentServiceImpl departmentService;
    @Autowired private LecturerServiceImpl lecturerService;
    @Autowired private RoomServiceImpl roomService;
    @Autowired private ProgramServiceImpl programService;
    @Autowired private CourseServiceImpl courseService;
    @Autowired private ProgramCourseServiceImpl programCourseService;
    @Autowired private StudentServiceImpl studentService;
    @Autowired private SectionServiceImpl sectionService;
    @Autowired private EnrollmentServiceImpl enrollmentService;
    @Autowired private TimetableServiceImpl timetableService;
    @Autowired private TimeslotRepository timeslotRepository;

    @Autowired private SchoolRepository schoolRepository;
    @Autowired private AcademicPeriodRepository academicPeriodRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private ProgramRepository programRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private SectionRepository sectionRepository;
    @Autowired private SectionLecturerRepository sectionLecturerRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private ProgramCourseRepository programCourseRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private StudentMapper studentMapper;

    private School school;
    private AcademicPeriod period;

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "upload.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

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
        p.setAcademicPeriodName("2026/2027 First");
        period = academicPeriodRepository.save(p);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void uploadMasterData() {
        departmentService.bulkUploadDepartments(csv("""
                departmentCode,departmentName
                CSC,Computer Science
                MTH,Mathematics
                """), false);
        lecturerService.bulkUploadLecturers(csv("""
                lecturerStaffNumber,lecturerFirstName,lecturerLastName,lecturerEmail,departmentCode
                STF1,Ada,Obi,ada@uni.test,CSC
                STF2,Bayo,Ade,bayo@uni.test,MTH
                """), false);
        roomService.bulkUploadRooms(csv("""
                roomNumber,roomBuilding,roomCapacity,roomType
                LT1,Main,200,lecture theatre
                LAB1,Main,40,LAB
                """), false);
        programService.bulkUploadPrograms(csv("""
                programCode,programName,departmentCode,programLevel,programDuration
                BSC-CSC,BSc Computer Science,CSC,UNDERGRADUATE,4
                """), false);
        courseService.bulkUploadCourses(csv("""
                courseCode,courseName,courseUnit,courseLevel,departmentCode
                CSC101,Intro to Computing,3,100,CSC
                MTH101,Calculus I,3,LEVEL_100,MTH
                """), false);
    }

    @Test
    @DisplayName("the full sequence: master data, curriculum, students, sections, enrollments")
    void fullSequence() {
        uploadMasterData();

        BulkUploadResponse curriculum = programCourseService.bulkUploadProgramCourses(csv("""
                programCode,courseCode,level,semester,isCore
                BSC-CSC,CSC101,100,FIRST,yes
                BSC-CSC,MTH101,100,FIRST,no
                """), false);
        assertThat(curriculum.getData().getCreated()).isEqualTo(2);

        BulkUploadResponse students = studentService.bulkUploadStudents(csv("""
                studentMatriculationNumber,studentFirstName,studentLastName,studentEmail,studentLevel,programCode,departmentCode
                M001,Chidi,Eze,chidi@uni.test,100,BSC-CSC,
                M002,Dayo,Ola,dayo@uni.test,LEVEL_100,,MTH
                """), false);
        assertThat(students.getData().getErrors()).isEmpty();
        assertThat(students.getData().getCreated()).isEqualTo(2);

        transactionTemplate.executeWithoutResult(tx -> {
            Student chidi = studentRepository.findByMatriculationNumberForTenant("M001", school.getId()).orElseThrow();
            assertThat(chidi.getStudentProgram().getProgramCode()).isEqualTo("BSC-CSC");
            assertThat(chidi.getStudentDepartment().getDepartmentCode()).isEqualTo("CSC");
            Student dayo = studentRepository.findByMatriculationNumberForTenant("M002", school.getId()).orElseThrow();
            assertThat(dayo.getStudentProgram()).isNull();
            assertThat(dayo.getStudentDepartment().getDepartmentCode()).isEqualTo("MTH");
        });

        // Both courses have a section "A": the enrollment must land in the right course's one.
        BulkUploadResponse sections = sectionService.bulkUploadSections(csv("""
                courseCode,sectionName,sectionEnrollmentSize,lecturerStaffNumbers,sectionDurationMinutes,eventType,rooms,timeslot
                CSC101,A,40,STF1,180,CLASS,LT1/LAB1,M(08:00-09:00)
                MTH101,A,40,STF2,60,CLASS,,
                """), period.getId(), false);
        assertThat(sections.getData().getErrors()).isEmpty();
        assertThat(sections.getData().getCreated()).isEqualTo(2);
        assertThat(sectionsInPeriod()).hasSize(2);

        BulkUploadResponse enrollments = enrollmentService.bulkUploadEnrollments(csv("""
                studentMatriculationNumber,courseCode,sectionName
                M002,MTH101,A
                M001,CSC101,A
                M001,CSC101,A
                M999,CSC101,A
                """), period.getId(), false);
        assertThat(enrollments.getData().getCreated()).isEqualTo(2);
        assertThat(enrollments.getData().getSkipped()).isEqualTo(2);
        assertThat(enrollments.getData().getErrors()).extracting(BulkUploadResponse.RowIssue::getRow)
                .containsExactly(4, 5);

        transactionTemplate.executeWithoutResult(tx -> {
            List<Enrollment> all = enrollmentRepository.findAllLiveByAcademicPeriod(school.getId(), period.getId());
            assertThat(all).extracting(e -> e.getEnrollmentStudent().getStudentMatriculationNumber()
                            + "->" + e.getEnrollmentSection().getSectionCourse().getCourseCode())
                    .containsExactlyInAnyOrder("M002->MTH101", "M001->CSC101");
        });

        // Uploading the same enrollments again changes nothing.
        BulkUploadResponse again = enrollmentService.bulkUploadEnrollments(csv("""
                studentMatriculationNumber,courseCode,sectionName
                M002,MTH101,A
                """), period.getId(), false);
        assertThat(again.getData().getUnchanged()).isEqualTo(1);
        assertThat(again.getData().getCreated()).isZero();
    }

    @Test
    @DisplayName("names are optional: without them people are shown by matriculation or staff number")
    void namesAreOptional() {
        departmentService.bulkUploadDepartments(csv("""
                departmentCode,departmentName
                CSC,Computer Science
                """), false);
        BulkUploadResponse lecturers = lecturerService.bulkUploadLecturers(csv("""
                lecturerStaffNumber,departmentCode
                STF9,CSC
                """), false);
        assertThat(lecturers.getData().getErrors()).isEmpty();
        assertThat(lecturers.getData().getCreated()).isEqualTo(1);

        BulkUploadResponse students = studentService.bulkUploadStudents(csv("""
                studentMatriculationNumber,studentFirstName,studentLastName,studentEmail,studentLevel,departmentCode
                M900,,,,100,CSC
                M901,Chidi,Eze,chidi@uni.test,100,CSC
                """), false);
        assertThat(students.getData().getErrors()).isEmpty();
        assertThat(students.getData().getCreated()).isEqualTo(2);

        assertThat(studentMapper.toResponse(studentRepository
                .findByMatriculationNumberForTenant("M900", school.getId()).orElseThrow())
                .getStudentDisplayName()).isEqualTo("M900");
        assertThat(studentRepository.findByMatriculationNumberForTenant("M901", school.getId())
                .orElseThrow().displayName()).isEqualTo("Chidi Eze");

        // Leaving the columns out keeps what was stored.
        studentService.bulkUploadStudents(csv("""
                studentMatriculationNumber,studentLevel,departmentCode
                M901,200,CSC
                """), false);
        Student kept = studentRepository.findByMatriculationNumberForTenant("M901", school.getId()).orElseThrow();
        assertThat(kept.displayName()).isEqualTo("Chidi Eze");
        assertThat(kept.getStudentEmail()).isEqualTo("chidi@uni.test");

        // Blank cells strip it.
        studentService.bulkUploadStudents(csv("""
                studentMatriculationNumber,studentFirstName,studentLastName,studentEmail,studentLevel,departmentCode
                M901,,,,100,CSC
                """), false);
        Student stripped = studentRepository.findByMatriculationNumberForTenant("M901", school.getId()).orElseThrow();
        assertThat(stripped.getStudentFirstName()).isNull();
        assertThat(stripped.getStudentLastName()).isNull();
        assertThat(stripped.getStudentEmail()).isNull();
        assertThat(stripped.displayName()).isEqualTo("M901");
    }

    @Test
    @DisplayName("a section timeslot overlapping the school's break rejects the row")
    void breakTimeslotRejected() {
        school.setSchoolBreakStartHour(LocalTime.of(12, 0));
        school.setSchoolBreakEndHour(LocalTime.of(13, 0));
        school = schoolRepository.save(school);
        uploadMasterData();

        BulkUploadResponse sections = sectionService.bulkUploadSections(csv("""
                courseCode,sectionName,sectionEnrollmentSize,lecturerStaffNumbers,sectionDurationMinutes,eventType,rooms,timeslot
                CSC101,A,40,STF1,60,CLASS,LT1,M(11:00-12:00)
                MTH101,A,40,STF2,60,CLASS,LT1,M(12:30-13:30)
                """), period.getId(), false);
        assertThat(sections.getData().getCreated()).isEqualTo(1);
        assertThat(sections.getData().getErrors())
                .extracting(BulkUploadResponse.RowIssue::getRow, BulkUploadResponse.RowIssue::getColumn)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(3, "timeslot"));
    }

    @Test
    @DisplayName("a dry run reports what would happen and saves nothing")
    void dryRunSavesNothing() {
        BulkUploadResponse response = departmentService.bulkUploadDepartments(csv("""
                departmentCode,departmentName
                CSC,Computer Science
                PHY,Physics
                """), true);

        assertThat(response.getData().isDryRun()).isTrue();
        assertThat(response.getData().getCreated()).isEqualTo(2);
        assertThat(departmentRepository.findAllBySchool_Id(school.getId())).isEmpty();
    }

    @Test
    @DisplayName("bad rows are skipped and reported while good rows are saved")
    void partialCommit() {
        departmentService.bulkUploadDepartments(csv("departmentCode,departmentName\nCSC,Computer Science\n"), false);

        BulkUploadResponse response = courseService.bulkUploadCourses(csv("""
                courseCode,courseName,courseUnit,courseLevel,departmentCode
                CSC101,Intro,3,100,CSC
                CSC102,Data Structures,3,100,NOPE
                CSC103,Algorithms,3,level nine,CSC
                CSC101,Intro again,3,100,CSC
                """), false);

        BulkUploadResponse.Data data = response.getData();
        assertThat(data.getCreated()).isEqualTo(1);
        assertThat(data.getSkipped()).isEqualTo(3);
        assertThat(data.getErrors()).extracting(BulkUploadResponse.RowIssue::getRow, BulkUploadResponse.RowIssue::getColumn)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(3, "departmentCode"),
                        org.assertj.core.groups.Tuple.tuple(4, "courseLevel"),
                        org.assertj.core.groups.Tuple.tuple(5, "courseCode"));
        assertThat(courseRepository.findAllBySchool_Id(school.getId())).extracting(Course::getCourseCode)
                .containsExactly("CSC101");
    }

    @Test
    @DisplayName("re-uploading a section replaces its events and lecturers instead of adding to them")
    void reuploadReplacesSection() {
        uploadMasterData();
        String header = "courseCode,sectionName,lecturerStaffNumbers,sectionDurationMinutes,eventType,split\n";
        BulkUploadResponse first = sectionService.bulkUploadSections(csv(header
                + "CSC101,A,STF1,180,CLASS,yes\n"
                + "CSC101,A,STF1,60,CLASS,\n"), period.getId(), false);
        assertThat(first.getData().getErrors()).singleElement()
                .satisfies(e -> assertThat(e.getMessage()).contains("already on row 2"));

        BulkUploadResponse second = sectionService.bulkUploadSections(csv(header
                + "CSC101,A,STF2,120,LAB\n"), period.getId(), false);
        assertThat(second.getData().getUpdated()).isEqualTo(1);

        List<Section> sections = sectionsInPeriod();
        assertThat(sections).hasSize(1);
        UUID sectionId = sections.get(0).getId();
        List<Event> events = eventRepository.findEventByFilter(school.getId(), null, sectionId, null, null);
        assertThat(events).singleElement().satisfies(e -> assertThat(e.getEventDuration().toMinutes()).isEqualTo(120));
        transactionTemplate.executeWithoutResult(tx -> assertThat(
                sectionLecturerRepository.findSectionLecturerByFilter(school.getId(), null, sectionId, null))
                .extracting(SectionLecturer::getSectionLecturerLecturer)
                .extracting(l -> l.getLecturerStaffNumber())
                .containsExactly("STF2"));
    }

    @Test
    @DisplayName("copies of a section left by the old upload are merged, keeping their enrollments")
    void mergesLegacyDuplicateSections() {
        uploadMasterData();
        studentService.bulkUploadStudents(csv("""
                studentMatriculationNumber,studentFirstName,studentLastName,studentEmail,studentLevel,programCode
                M001,Chidi,Eze,chidi@uni.test,100,BSC-CSC
                """), false);
        Course course = courseRepository.findAllBySchool_Id(school.getId()).stream()
                .filter(c -> c.getCourseCode().equals("CSC101")).findFirst().orElseThrow();
        Section first = legacySection(course);
        Section copy = legacySection(course);
        Enrollment enrollment = new Enrollment();
        enrollment.setSchool(school);
        enrollment.setEnrollmentSection(copy);
        enrollment.setEnrollmentStudent(studentRepository.findByMatriculationNumberForTenant("M001", school.getId()).orElseThrow());
        enrollmentRepository.save(enrollment);

        BulkUploadResponse response = sectionService.bulkUploadSections(csv("""
                courseCode,sectionName,lecturerStaffNumbers,sectionDurationMinutes,eventType
                CSC101,A,STF1,60,CLASS
                """), period.getId(), false);

        assertThat(response.getData().getWarnings()).singleElement()
                .satisfies(w -> assertThat(w.getMessage()).contains("1 duplicate"));
        List<Section> remaining = sectionsInPeriod();
        assertThat(remaining).extracting(Section::getId).containsAnyOf(first.getId(), copy.getId()).hasSize(1);
        UUID kept = remaining.get(0).getId();
        assertThat(eventRepository.findEventByFilter(school.getId(), null, kept, null, null)).hasSize(1);
        transactionTemplate.executeWithoutResult(tx -> assertThat(
                enrollmentRepository.findAllLiveByAcademicPeriod(school.getId(), period.getId()))
                .singleElement()
                .satisfies(e -> assertThat(e.getEnrollmentSection().getId()).isEqualTo(kept)));
    }

    @Test
    @DisplayName("a programme created before codes existed is adopted by name, not duplicated")
    void adoptsLegacyProgramme() {
        departmentService.bulkUploadDepartments(csv("departmentCode,departmentName\nCSC,Computer Science\n"), false);
        Department csc = departmentRepository.findAllBySchool_Id(school.getId()).get(0);
        Program legacy = new Program();
        legacy.setSchool(school);
        legacy.setProgramName("BSc Computer Science");
        legacy.setProgramDepartment(csc);
        programRepository.save(legacy);

        BulkUploadResponse response = programService.bulkUploadPrograms(csv("""
                programCode,programName,departmentCode
                bsc-csc,bsc computer science,CSC
                """), false);

        assertThat(response.getData().getUpdated()).isEqualTo(1);
        assertThat(programRepository.findAllBySchool_Id(school.getId())).singleElement()
                .satisfies(p -> {
                    assertThat(p.getId()).isEqualTo(legacy.getId());
                    assertThat(p.getProgramCode()).isEqualTo("BSC-CSC");
                });
    }

    @Test
    @DisplayName("curriculum rows upsert on programme and course")
    void curriculumUpserts() {
        uploadMasterData();
        String header = "programCode,courseCode,level,isCore\n";
        programCourseService.bulkUploadProgramCourses(csv(header + "BSC-CSC,CSC101,100,true\n"), false);
        BulkUploadResponse second = programCourseService.bulkUploadProgramCourses(
                csv(header + "BSC-CSC,CSC101,200,false\n"), false);

        assertThat(second.getData().getUpdated()).isEqualTo(1);
        assertThat(programCourseRepository.findProgramCourseByFilter(school.getId(), null, null, null, null))
                .singleElement()
                .satisfies(pc -> {
                    assertThat(pc.getProgramCourseLevel()).isEqualTo(CourseEnum.CourseLevel.LEVEL_200);
                    assertThat(pc.getProgramCourseIsCore()).isFalse();
                });
    }

    private Section legacySection(Course course) {
        Section section = new Section();
        section.setSchool(school);
        section.setSectionCourse(course);
        section.setSectionName("A");
        section.setSectionAcademicPeriod(period);
        return sectionRepository.save(section);
    }

    private List<Long> eventMinutes(String courseCode, String sectionName) {
        Section section = sectionsInPeriod().stream()
                .filter(sec -> sec.getSectionCourse().getCourseCode().equals(courseCode)
                        && sec.getSectionName().equals(sectionName))
                .findFirst().orElseThrow();
        return eventRepository.findEventByFilter(school.getId(), null, section.getId(), null, null).stream()
                .map(e -> e.getEventDuration().toMinutes()).sorted().toList();
    }

    @Test
    @DisplayName("split=yes divides 3-unit courses 2/3 + 1/3 and 4-unit courses in half; other units are rejected")
    void splitFollowsUnits() {
        uploadMasterData();
        courseService.bulkUploadCourses(csv("""
                courseCode,courseName,courseUnit,courseLevel,departmentCode
                CSC201,Data Structures,4,200,CSC
                CSC202,Ethics,2,200,CSC
                """), false);

        BulkUploadResponse response = sectionService.bulkUploadSections(csv("""
                courseCode,sectionName,lecturerStaffNumbers,sectionDurationMinutes,eventType,split
                CSC101,A,STF1,180,CLASS,yes
                CSC201,A,STF1,120,CLASS,YES
                MTH101,A,STF2,180,CLASS,no
                CSC202,A,STF1,120,CLASS,yes
                CSC101,B,STF1,100,CLASS,yes
                CSC202,B,STF1,60,CLASS,maybe
                """), period.getId(), false);

        assertThat(response.getData().getErrors())
                .extracting(BulkUploadResponse.RowIssue::getRow, BulkUploadResponse.RowIssue::getColumn)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(5, "split"),
                        org.assertj.core.groups.Tuple.tuple(6, "sectionDurationMinutes"));
        assertThat(response.getData().getWarnings()).extracting(BulkUploadResponse.RowIssue::getRow).containsExactly(7);
        assertThat(eventMinutes("CSC101", "A")).containsExactly(60L, 120L);
        assertThat(eventMinutes("CSC201", "A")).containsExactly(60L, 60L);
        assertThat(eventMinutes("MTH101", "A")).containsExactly(180L);
        assertThat(eventMinutes("CSC202", "B")).containsExactly(60L);
    }

    @Test
    @DisplayName("sectionUnits overrides the course's units for splitting, as when one course is taught at two weights")
    void sectionUnitsOverrideCourseUnits() {
        uploadMasterData();  // CSC101 and MTH101 are 3-unit courses
        BulkUploadResponse response = sectionService.bulkUploadSections(csv("""
                courseCode,sectionName,lecturerStaffNumbers,sectionDurationMinutes,sectionUnits,eventType,split
                CSC101,A,STF1,180,,CLASS,yes
                CSC101,B,STF1,120,4,CLASS,yes
                MTH101,A,STF2,60,1,CLASS,yes
                """), period.getId(), false);

        assertThat(response.getData().getErrors()).singleElement().satisfies(e -> {
            assertThat(e.getRow()).isEqualTo(4);
            assertThat(e.getColumn()).isEqualTo("split");
        });
        assertThat(eventMinutes("CSC101", "A")).containsExactly(60L, 120L);
        assertThat(eventMinutes("CSC101", "B")).containsExactly(60L, 60L);
        assertThat(sectionsInPeriod()).filteredOn(sec -> sec.getSectionName().equals("B"))
                .singleElement().satisfies(sec -> assertThat(sec.getSectionUnits()).isEqualTo(4));
    }

    @Test
    @DisplayName("relatedTo teaches the row's section in the named section's events, until released")
    void relatedToPairsSections() {
        uploadMasterData();
        String header = "courseCode,sectionName,lecturerStaffNumbers,sectionDurationMinutes,eventType,relatedTo\n";

        // Any two sections can pair, not only ones of the same group.
        BulkUploadResponse paired = sectionService.bulkUploadSections(csv(header
                + "CSC101,CSC101-X,STF1,120,CLASS,MTH101-Y\n"
                + "MTH101,MTH101-Y,STF2,60,CLASS,\n"), period.getId(), false);
        assertThat(paired.getData().getErrors()).isEmpty();
        assertThat(eventMinutes("CSC101", "CSC101-X")).isEmpty();
        assertThat(eventMinutes("MTH101", "MTH101-Y")).containsExactly(60L);

        // CSC101-X is already taught as part of MTH101-Y, so MTH101-Y cannot be related onward.
        BulkUploadResponse chain = sectionService.bulkUploadSections(csv(header
                + "MTH101,MTH101-Y,STF2,60,CLASS,CSC101-X\n"), period.getId(), false);
        assertThat(chain.getData().getErrors()).singleElement().satisfies(e -> {
            assertThat(e.getColumn()).isEqualTo("relatedTo");
            assertThat(e.getMessage()).contains("only pairs");
        });

        sectionService.bulkUploadSections(csv(header + "CSC101,CSC101-X,STF1,120,CLASS,\n"), period.getId(), false);
        assertThat(eventMinutes("CSC101", "CSC101-X")).containsExactly(120L);
        assertThat(eventMinutes("MTH101", "MTH101-Y")).containsExactly(60L);
    }

    @Test
    @DisplayName("relatedTo must name exactly one section in the period")
    void relatedToNeedsOneSection() {
        uploadMasterData();
        BulkUploadResponse response = sectionService.bulkUploadSections(csv("""
                courseCode,sectionName,lecturerStaffNumbers,sectionDurationMinutes,eventType,relatedTo
                CSC101,A,STF1,60,CLASS,
                MTH101,A,STF2,60,CLASS,
                CSC101,B,STF1,60,CLASS,A
                MTH101,B,STF2,60,CLASS,MTH101
                """), period.getId(), false);

        assertThat(response.getData().getErrors()).extracting(e -> e.getRow(), e -> e.getColumn()).containsExactly(
                org.assertj.core.groups.Tuple.tuple(4, "relatedTo"), org.assertj.core.groups.Tuple.tuple(5, "relatedTo"));
        assertThat(response.getData().getErrors().get(0).getMessage()).contains("Several sections have this name");
        assertThat(response.getData().getErrors().get(1).getMessage()).contains("No section has this name");
    }

    @Test
    @DisplayName("a pair only gets rooms seating both groups, and is listed as unassigned with the reason when none does")
    void pairNeedsRoomSeatingBoth() {
        uploadMasterData();
        sectionService.bulkUploadSections(csv("""
                courseCode,sectionName,sectionEnrollmentSize,lecturerStaffNumbers,sectionDurationMinutes,eventType,relatedTo
                CSC101,CSC101-A,30,STF1,60,CLASS,MTH101-A
                MTH101,MTH101-A,30,STF2,60,CLASS,
                CSC101,CSC101-B,150,STF1,60,CLASS,MTH101-B
                MTH101,MTH101-B,100,STF2,60,CLASS,
                """), period.getId(), false);
        timetableService.prepareEvents(school.getId(), period.getId());
        timetableService.prepareTimeslots(school.getId(), period.getId());

        CspModel model = timetableService.buildModel(school.getId(), period.getId()).orElseThrow();
        for (int e = 0; e < model.eventCount(); e++) {
            String name = model.event(e).getEventSection().getSectionName();
            // A: 60 students, so LT1 (200) but not LAB1 (40). B: 250, so no room at all.
            assertThat(model.domainOf(e).stream().map(c -> c.room().getRoomNumber()).distinct().toList())
                    .isEqualTo(name.equals("MTH101-A") ? List.of("LT1") : List.of());
        }

        assertThat(timetableService.buildTimetableResponse(school.getId(), period.getId()).getUnassigned())
                .filteredOn(u -> "CSC101-B/MTH101-B".equals(u.sectionName()))
                .singleElement()
                .satisfies(u -> assertThat(u.reason())
                        .isEqualTo("No room large enough for CSC101/MTH101: it needs 250 seats and the largest room holds 200"));
    }

    @Test
    @DisplayName("generation sizes the default slots to the events and shows a related pair as A/B")
    void generationPreparesSlotsAndLabels() {
        school.setSchoolBreakStartHour(LocalTime.of(12, 0));
        school.setSchoolBreakEndHour(LocalTime.of(13, 0));
        school = schoolRepository.save(school);
        uploadMasterData();
        sectionService.bulkUploadSections(csv("""
                courseCode,sectionName,lecturerStaffNumbers,sectionDurationMinutes,eventType,relatedTo
                CSC101,CSC101-A,STF1,60,CLASS,MTH101-A
                MTH101,MTH101-A,STF2,90,CLASS,
                """), period.getId(), false);

        timetableService.prepareEvents(school.getId(), period.getId());
        timetableService.prepareTimeslots(school.getId(), period.getId());

        List<Timeslot> slots = timeslotRepository.findAllBySchool_Id(school.getId());
        // 08:00–12:00 and 13:00–18:00 in 30-minute slots, Monday to Friday.
        assertThat(slots).hasSize(5 * 18)
                .allSatisfy(t -> assertThat(t.getTimeslotDuration().toMinutes()).isEqualTo(30))
                .noneSatisfy(t -> assertThat(t.getTimeslotStartTime()).isEqualTo(LocalTime.of(12, 0)));

        transactionTemplate.executeWithoutResult(tx -> {
            Event event = eventRepository.findAllBySchoolIdAndAcademicPeriodId(school.getId(), period.getId()).get(0);
            event.setEventTimeslot(slots.get(0));
        });
        assertThat(timetableService.buildTimetableResponse(school.getId(), period.getId()).getEntries())
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.getCourseCode()).isEqualTo("CSC101/MTH101");
                    assertThat(e.getSectionName()).isEqualTo("CSC101-A/MTH101-A");
                    assertThat(e.getCourseName()).isEqualTo("Intro to Computing/Calculus I");
                });
    }

    @Test
    @DisplayName("generation reports how each split=maybe section was planned")
    void maybeDecisionsAreReported() {
        uploadMasterData();
        sectionService.bulkUploadSections(csv("""
                courseCode,sectionName,lecturerStaffNumbers,sectionDurationMinutes,eventType,split
                CSC101,A,STF1,180,CLASS,maybe
                MTH101,A,STF2,180,CLASS,no
                """), period.getId(), false);

        List<SplitDecisionDTO> decisions = timetableService.prepareEvents(school.getId(), period.getId());

        assertThat(decisions).singleElement().satisfies(d -> {
            assertThat(d.courseCode()).isEqualTo("CSC101");
            assertThat(d.eventMinutes()).isEqualTo(d.split() ? List.of(120L, 60L) : List.of(180L));
            assertThat(eventMinutes("CSC101", "A")).containsExactlyInAnyOrderElementsOf(d.eventMinutes());
        });
    }

    @Test
    @DisplayName("the single-section create endpoint plans events and relates sections like the upload")
    void createSectionPlansAndRelates() {
        uploadMasterData();
        sectionService.bulkUploadSections(csv("""
                courseCode,sectionName,lecturerStaffNumbers,sectionDurationMinutes,eventType
                MTH101,A,STF2,60,CLASS
                """), period.getId(), false);
        UUID csc101 = courseRepository.findAllBySchool_Id(school.getId()).stream()
                .filter(c -> c.getCourseCode().equals("CSC101")).findFirst().orElseThrow().getId();
        UUID mth101A = sectionsInPeriod().get(0).getId();

        CreateSectionRequest request = new CreateSectionRequest();
        request.setSectionCourseId(csc101);
        request.setSectionName("A");
        request.setSectionAcademicPeriodId(period.getId());
        request.setSectionDurationMinutes(180);
        request.setSectionSplit(SectionEnum.SectionSplit.YES);
        request.setRelatedToSectionId(mth101A);
        assertThat(sectionService.createSection(request).getData().getSection().getSectionSplit())
                .isEqualTo(SectionEnum.SectionSplit.YES);

        assertThat(eventMinutes("CSC101", "A")).isEmpty();
        assertThat(eventMinutes("MTH101", "A")).containsExactly(60L);

        request.setSectionName("B");
        request.setRelatedToSectionId(null);
        request.setSectionDurationMinutes(100);
        assertThatThrownBy(() -> sectionService.createSection(request))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("sectionDurationMinutes");
    }

    @Test
    @DisplayName("updating a section re-plans its events and can pair and release it")
    void updateSectionReplansAndPairs() {
        uploadMasterData();
        sectionService.bulkUploadSections(csv("""
                courseCode,sectionName,lecturerStaffNumbers,sectionDurationMinutes,eventType
                CSC101,A,STF1,180,CLASS
                MTH101,A,STF2,60,CLASS
                """), period.getId(), false);
        Section csc = sectionsInPeriod().stream()
                .filter(sec -> sec.getSectionCourse().getCourseCode().equals("CSC101")).findFirst().orElseThrow();
        UUID mth101A = sectionsInPeriod().stream()
                .filter(sec -> sec.getSectionCourse().getCourseCode().equals("MTH101")).findFirst().orElseThrow().getId();

        UpdateSectionRequest split = new UpdateSectionRequest();
        split.setId(csc.getId());
        split.setSectionSplit(SectionEnum.SectionSplit.YES);
        sectionService.updateSection(split);
        assertThat(eventMinutes("CSC101", "A")).containsExactly(60L, 120L);

        UpdateSectionRequest pair = new UpdateSectionRequest();
        pair.setId(csc.getId());
        pair.setRelatedToSectionId(mth101A);
        sectionService.updateSection(pair);
        assertThat(eventMinutes("CSC101", "A")).isEmpty();
        assertThat(eventMinutes("MTH101", "A")).containsExactly(60L);

        UpdateSectionRequest rename = new UpdateSectionRequest();
        rename.setId(csc.getId());
        rename.setSectionName("Z");
        assertThatThrownBy(() -> sectionService.updateSection(rename))
                .hasMessageContaining("related pair");

        UpdateSectionRequest release = new UpdateSectionRequest();
        release.setId(csc.getId());
        release.setRemoveRelatedTo(true);
        sectionService.updateSection(release);
        assertThat(eventMinutes("MTH101", "A")).containsExactly(60L);
        assertThat(eventMinutes("CSC101", "A")).containsExactly(60L, 120L);
    }

    private List<Section> sectionsInPeriod() {
        return sectionRepository.findSectionByFilter(school.getId(), null, null, null, period.getId(), null);
    }
}
