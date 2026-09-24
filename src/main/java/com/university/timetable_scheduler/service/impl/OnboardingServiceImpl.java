package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.bulk.BulkUploadSupport;
import com.university.timetable_scheduler.dto.request.course.BulkUploadCourseArrayRequest;
import com.university.timetable_scheduler.dto.request.department.BulkUploadDepartmentArrayRequest;
import com.university.timetable_scheduler.dto.request.enrollment.BulkUploadEnrollmentArrayRequest;
import com.university.timetable_scheduler.dto.request.lecturer.BulkUploadLecturerArrayRequest;
import com.university.timetable_scheduler.dto.request.program.BulkUploadProgramArrayRequest;
import com.university.timetable_scheduler.dto.request.programcourse.BulkUploadProgramCourseArrayRequest;
import com.university.timetable_scheduler.dto.request.room.BulkUploadRoomArrayRequest;
import com.university.timetable_scheduler.dto.request.section.BulkUploadSectionArrayRequest;
import com.university.timetable_scheduler.dto.request.student.BulkUploadStudentArrayRequest;
import com.university.timetable_scheduler.dto.response.onboarding.OnboardingGuideResponse;
import com.university.timetable_scheduler.repository.*;
import com.university.timetable_scheduler.service.OnboardingService;
import com.university.timetable_scheduler.status.BulkUploadEnum.BulkDataset;
import com.university.timetable_scheduler.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The onboarding guide: every bulk upload in {@link BulkDataset} order, the academic period the
 * per-period uploads need, and generation last. CSV columns are read off the upload row classes,
 * so the guide cannot drift from what the uploads accept.
 */
@Service
public class OnboardingServiceImpl implements OnboardingService {

    static final String ACADEMIC_PERIOD = "ACADEMIC_PERIOD";
    static final String GENERATE_TIMETABLE = "GENERATE_TIMETABLE";

    /** One step of the guide, before the school's progress is known. */
    private record StepDefinition(String key, BulkDataset dataset, String title, String description,
                                  String method, String endpoint, String arrayEndpoint, Class<?> rowType,
                                  List<String> dependsOn, TenantAwareRepository<?> countedIn) {
    }

    private final List<StepDefinition> definitions;

    public OnboardingServiceImpl(DepartmentRepository departmentRepository, LecturerRepository lecturerRepository,
                                 RoomRepository roomRepository, ProgramRepository programRepository,
                                 CourseRepository courseRepository, ProgramCourseRepository programCourseRepository,
                                 StudentRepository studentRepository, AcademicPeriodRepository academicPeriodRepository,
                                 SectionRepository sectionRepository, EnrollmentRepository enrollmentRepository) {
        this.definitions = List.of(
                upload(BulkDataset.DEPARTMENTS, "Upload departments",
                        "Everything else belongs to a department. Leave departmentHeadStaffNumber blank on the "
                                + "first upload, since lecturers do not exist yet, and re-upload once they do.",
                        "/api/departments", BulkUploadDepartmentArrayRequest.Row.class, List.of(),
                        departmentRepository),
                upload(BulkDataset.LECTURERS, "Upload lecturers",
                        "Each lecturer belongs to one department, given by departmentCode.",
                        "/api/lecturers", BulkUploadLecturerArrayRequest.Row.class,
                        List.of(BulkDataset.DEPARTMENTS.name()), lecturerRepository),
                upload(BulkDataset.ROOMS, "Upload rooms",
                        "Rooms stand alone. A room is identified by roomNumber and roomBuilding together.",
                        "/api/rooms", BulkUploadRoomArrayRequest.Row.class, List.of(), roomRepository),
                upload(BulkDataset.PROGRAMS, "Upload programmes",
                        "Each programme belongs to a department. programCoordinatorStaffNumber is optional "
                                + "and needs the lecturer to exist.",
                        "/api/programs", BulkUploadProgramArrayRequest.Row.class,
                        List.of(BulkDataset.DEPARTMENTS.name()), programRepository),
                upload(BulkDataset.COURSES, "Upload courses",
                        "Each course code appears once and belongs to one department, even when several "
                                + "programmes take it.",
                        "/api/courses", BulkUploadCourseArrayRequest.Row.class,
                        List.of(BulkDataset.DEPARTMENTS.name()), courseRepository),
                upload(BulkDataset.CURRICULUM, "Upload the curriculum",
                        "Which courses each programme takes, at which level and semester.",
                        "/api/program-courses", BulkUploadProgramCourseArrayRequest.Row.class,
                        List.of(BulkDataset.PROGRAMS.name(), BulkDataset.COURSES.name()), programCourseRepository),
                upload(BulkDataset.STUDENTS, "Upload students",
                        "Give each student a programCode; departmentCode is the fallback for a school "
                                + "without programmes.",
                        "/api/students", BulkUploadStudentArrayRequest.Row.class,
                        List.of(BulkDataset.PROGRAMS.name()), studentRepository),
                new StepDefinition(ACADEMIC_PERIOD, null, "Create an academic period",
                        "Sections and enrollments are uploaded into one academic period. Create it, and use "
                                + "its id as academicPeriodId in the next steps.",
                        "POST", "/api/academic-periods/create", null, null, List.of(), academicPeriodRepository),
                upload(BulkDataset.SECTIONS, "Upload sections and events",
                        "One row per event. The rows for a section are its whole definition: re-uploading a "
                                + "section replaces its events, lecturers, rooms and timeslots. rooms and "
                                + "timeslot are optional restrictions.",
                        "/api/sections", BulkUploadSectionArrayRequest.Row.class,
                        List.of(BulkDataset.COURSES.name(), BulkDataset.LECTURERS.name(),
                                BulkDataset.ROOMS.name(), ACADEMIC_PERIOD), sectionRepository),
                upload(BulkDataset.ENROLLMENTS, "Upload enrollments",
                        "Which student is in which section. Uploading an enrollment that already exists "
                                + "changes nothing.",
                        "/api/enrollments", BulkUploadEnrollmentArrayRequest.Row.class,
                        List.of(BulkDataset.STUDENTS.name(), BulkDataset.SECTIONS.name()), enrollmentRepository),
                new StepDefinition(GENERATE_TIMETABLE, null, "Generate the timetable",
                        "Queues a generation for the academic period. The result goes to the school's webhook, "
                                + "or poll the status url in the response.",
                        "POST", "/api/time-table/generate?academicPeriodId={academicPeriodId}", null, null,
                        List.of(BulkDataset.SECTIONS.name(), BulkDataset.ENROLLMENTS.name()), null));
    }

    /** A bulk upload step. The per-period datasets take the period as a query parameter. */
    private static StepDefinition upload(BulkDataset dataset, String title, String description, String basePath,
                                         Class<?> rowType, List<String> dependsOn,
                                         TenantAwareRepository<?> countedIn) {
        boolean perPeriod = dataset == BulkDataset.SECTIONS || dataset == BulkDataset.ENROLLMENTS;
        String endpoint = basePath + "/bulk-upload" + (perPeriod ? "?academicPeriodId={academicPeriodId}" : "");
        return new StepDefinition(dataset.name(), dataset, title, description, "POST", endpoint,
                basePath + "/bulk-upload/array", rowType, dependsOn, countedIn);
    }

    @Override
    public OnboardingGuideResponse readGuide() {
        UUID schoolId = TenantContext.getSchoolId();

        Map<String, Boolean> completedByKey = new HashMap<>();
        List<OnboardingGuideResponse.Step> steps = new ArrayList<>();
        for (StepDefinition definition : definitions) {
            Long count = definition.countedIn() == null ? null : definition.countedIn().countLiveBySchoolId(schoolId);
            Boolean completed = count == null ? null : count > 0;
            completedByKey.put(definition.key(), completed);

            List<String> required = definition.rowType() == null
                    ? List.of() : BulkUploadSupport.requiredColumns(definition.rowType());
            List<String> optional = definition.rowType() == null
                    ? List.of() : BulkUploadSupport.columns(definition.rowType()).stream()
                            .filter(column -> !required.contains(column)).toList();
            // Definitions only depend on earlier steps, so every dependency is already in the map.
            boolean ready = definition.dependsOn().stream().allMatch(k -> Boolean.TRUE.equals(completedByKey.get(k)));

            steps.add(new OnboardingGuideResponse.Step(steps.size() + 1, definition.key(), definition.dataset(),
                    definition.title(), definition.description(), definition.method(), definition.endpoint(),
                    definition.arrayEndpoint(), required, optional, definition.dependsOn(), count, completed, ready));
        }

        int nextStep = steps.stream()
                .filter(step -> Boolean.FALSE.equals(step.getCompleted()))
                .mapToInt(OnboardingGuideResponse.Step::getStep)
                .findFirst()
                .orElse(steps.size());

        OnboardingGuideResponse response = new OnboardingGuideResponse();
        response.setData(new OnboardingGuideResponse.Data(steps.size(),
                (int) steps.stream().filter(step -> Boolean.TRUE.equals(step.getCompleted())).count(),
                nextStep, steps));
        return response;
    }
}
