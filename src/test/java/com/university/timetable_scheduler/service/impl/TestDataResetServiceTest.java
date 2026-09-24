package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.entity.AcademicPeriod;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.repository.*;
import com.university.timetable_scheduler.tenant.TenantContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Against H2, so the delete order is checked by real foreign keys. */
@SpringBootTest(properties = "testing.reset-enabled=true")
class TestDataResetServiceTest {

    @Autowired private TestDataResetService resetService;
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
    @Autowired private LecturerRepository lecturerRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private ProgramRepository programRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private ProgramCourseRepository programCourseRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private SectionRepository sectionRepository;
    @Autowired private SectionLecturerRepository sectionLecturerRepository;
    @Autowired private SectionRoomRepository sectionRoomRepository;
    @Autowired private SectionTimeslotRepository sectionTimeslotRepository;
    @Autowired private TimeslotRepository timeslotRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private ActivityRepository activityRepository;

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "upload.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    /** A school with a row in every table the reset clears, including the department-head cycle. */
    private School populatedSchool() {
        School s = new School();
        String name = "school-" + UUID.randomUUID();
        s.setSchoolName(name);
        s.setSchoolAdminEmail("admin@" + name + ".test");
        s.setSchoolAdminPassword("irrelevant-hash");
        s.setSchoolAddress(name + " address");
        s.setSchoolPhone("phone-" + name);
        s.setSchoolDayStartHour(LocalTime.of(8, 0));
        s.setSchoolDayEndHour(LocalTime.of(18, 0));
        School school = schoolRepository.save(s);

        TenantContext.runWith(school.getId(), () -> {
            AcademicPeriod period = new AcademicPeriod();
            period.setSchool(school);
            period.setAcademicPeriodName("2026/2027 First");
            period = academicPeriodRepository.save(period);

            departmentService.bulkUploadDepartments(csv("departmentCode,departmentName\nCSC,Computer Science\n"), false);
            lecturerService.bulkUploadLecturers(csv("""
                    lecturerStaffNumber,lecturerFirstName,lecturerLastName,lecturerEmail,departmentCode
                    STF1,Ada,Obi,ada@uni.test,CSC
                    """), false);
            departmentService.bulkUploadDepartments(csv("""
                    departmentCode,departmentName,departmentHeadStaffNumber
                    CSC,Computer Science,STF1
                    """), false);
            roomService.bulkUploadRooms(csv("roomNumber,roomBuilding,roomCapacity,roomType\nLT1,Main,200,CLASS\n"), false);
            programService.bulkUploadPrograms(csv("""
                    programCode,programName,departmentCode,programCoordinatorStaffNumber
                    BSC-CSC,BSc Computer Science,CSC,STF1
                    """), false);
            courseService.bulkUploadCourses(csv("""
                    courseCode,courseName,courseUnit,courseLevel,departmentCode
                    CSC101,Intro to Computing,3,100,CSC
                    """), false);
            programCourseService.bulkUploadProgramCourses(csv("programCode,courseCode,level\nBSC-CSC,CSC101,100\n"), false);
            studentService.bulkUploadStudents(csv("""
                    studentMatriculationNumber,studentFirstName,studentLastName,studentEmail,studentLevel,programCode
                    M001,Chidi,Eze,chidi@uni.test,100,BSC-CSC
                    """), false);
            sectionService.bulkUploadSections(csv("""
                    courseCode,sectionName,lecturerStaffNumbers,eventDurationMinutes,eventType,rooms,timeslot
                    CSC101,A,STF1,60,CLASS,LT1,M(08:00-09:00)
                    """), period.getId(), false);
            enrollmentService.bulkUploadEnrollments(csv("studentMatriculationNumber,courseCode,sectionName\nM001,CSC101,A\n"),
                    period.getId(), false);
        });
        return school;
    }

    private List<Long> counts(UUID schoolId) {
        return List.of(academicPeriodRepository, departmentRepository, lecturerRepository, roomRepository,
                        programRepository, courseRepository, programCourseRepository, studentRepository,
                        sectionRepository, sectionLecturerRepository, sectionRoomRepository, sectionTimeslotRepository,
                        timeslotRepository, eventRepository, enrollmentRepository, activityRepository)
                .stream().map(repository -> repository.countLiveBySchoolId(schoolId)).toList();
    }

    @Test
    @DisplayName("clears every table for the caller's school, keeps the school, and leaves other schools alone")
    void resetsOnlyTheCallersSchool() {
        School target = populatedSchool();
        School other = populatedSchool();
        assertThat(counts(target.getId())).allMatch(count -> count > 0);

        Map<String, Integer> deleted = TenantContext.callWith(target.getId(), resetService::resetCurrentSchool);

        assertThat(deleted).containsEntry("Enrollment", 1).containsEntry("Department", 1);
        assertThat(counts(target.getId())).allMatch(count -> count == 0);
        assertThat(schoolRepository.findById(target.getId())).isPresent();
        assertThat(counts(other.getId())).allMatch(count -> count > 0);
    }
}
