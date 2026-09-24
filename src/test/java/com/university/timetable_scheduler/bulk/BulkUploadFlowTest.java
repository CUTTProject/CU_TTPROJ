package com.university.timetable_scheduler.bulk;

import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
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
import com.university.timetable_scheduler.repository.*;
import com.university.timetable_scheduler.service.impl.*;
import com.university.timetable_scheduler.status.CourseEnum;
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
                courseCode,sectionName,sectionEnrollmentSize,lecturerStaffNumbers,eventDurationMinutes,eventType,rooms,timeslot
                CSC101,A,40,STF1,60,CLASS,LT1,M(08:00-09:00)
                CSC101,A,40,STF1,120,LAB,LAB1,
                MTH101,A,40,STF2,60,CLASS,,
                """), period.getId(), false);
        assertThat(sections.getData().getErrors()).isEmpty();
        assertThat(sections.getData().getCreated()).isEqualTo(3);
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
        String header = "courseCode,sectionName,lecturerStaffNumbers,eventDurationMinutes,eventType\n";
        sectionService.bulkUploadSections(csv(header
                + "CSC101,A,STF1,60,CLASS\n"
                + "CSC101,A,STF1,60,CLASS\n"), period.getId(), false);

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
                courseCode,sectionName,lecturerStaffNumbers,eventDurationMinutes,eventType
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

    private List<Section> sectionsInPeriod() {
        return sectionRepository.findSectionByFilter(school.getId(), null, null, null, period.getId(), null);
    }
}
