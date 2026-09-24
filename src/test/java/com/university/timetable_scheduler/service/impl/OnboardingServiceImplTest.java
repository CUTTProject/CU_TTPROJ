package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.response.onboarding.OnboardingGuideResponse;
import com.university.timetable_scheduler.repository.*;
import com.university.timetable_scheduler.status.BulkUploadEnum.BulkDataset;
import com.university.timetable_scheduler.tenant.TenantContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OnboardingServiceImplTest {

    private final UUID schoolId = UUID.randomUUID();
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final LecturerRepository lecturers = mock(LecturerRepository.class);
    private final RoomRepository rooms = mock(RoomRepository.class);
    private final ProgramRepository programs = mock(ProgramRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final ProgramCourseRepository programCourses = mock(ProgramCourseRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final AcademicPeriodRepository periods = mock(AcademicPeriodRepository.class);
    private final SectionRepository sections = mock(SectionRepository.class);
    private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);

    private final OnboardingServiceImpl service = new OnboardingServiceImpl(departments, lecturers, rooms, programs,
            courses, programCourses, students, periods, sections, enrollments);

    private OnboardingGuideResponse.Data guide() {
        return TenantContext.callWith(schoolId, service::readGuide).getData();
    }

    private static OnboardingGuideResponse.Step step(OnboardingGuideResponse.Data data, String key) {
        return data.getSteps().stream().filter(s -> s.getKey().equals(key)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("every dataset appears in upload order, with the period before sections and generation last")
    void listsStepsInOrder() {
        OnboardingGuideResponse.Data data = guide();

        List<String> expected = new ArrayList<>();
        for (BulkDataset dataset : BulkDataset.values()) {
            if (dataset == BulkDataset.SECTIONS) expected.add(OnboardingServiceImpl.ACADEMIC_PERIOD);
            expected.add(dataset.name());
        }
        expected.add(OnboardingServiceImpl.GENERATE_TIMETABLE);

        assertThat(data.getSteps()).extracting(OnboardingGuideResponse.Step::getKey).containsExactlyElementsOf(expected);
        assertThat(data.getSteps()).extracting(OnboardingGuideResponse.Step::getStep)
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, expected.size()).boxed().toList());
        assertThat(data.getTotalSteps()).isEqualTo(expected.size());
    }

    @Test
    @DisplayName("a step only depends on steps before it")
    void dependenciesPointBackwards() {
        List<OnboardingGuideResponse.Step> steps = guide().getSteps();
        for (int i = 0; i < steps.size(); i++) {
            List<String> earlier = steps.subList(0, i).stream().map(OnboardingGuideResponse.Step::getKey).toList();
            assertThat(earlier).as(steps.get(i).getKey()).containsAll(steps.get(i).getDependsOn());
        }
    }

    @Test
    @DisplayName("CSV columns come from the upload row class, split into required and optional")
    void describesColumns() {
        OnboardingGuideResponse.Step sectionsStep = step(guide(), BulkDataset.SECTIONS.name());

        assertThat(sectionsStep.getRequiredColumns()).containsExactly(
                "courseCode", "sectionName", "lecturerStaffNumbers", "eventDurationMinutes", "eventType");
        assertThat(sectionsStep.getOptionalColumns()).containsExactly("sectionEnrollmentSize", "rooms", "timeslot");
        assertThat(sectionsStep.getEndpoint()).isEqualTo("/api/sections/bulk-upload?academicPeriodId={academicPeriodId}");
        assertThat(step(guide(), OnboardingServiceImpl.ACADEMIC_PERIOD).getRequiredColumns()).isEmpty();
    }

    @Test
    @DisplayName("progress follows the school's counts: done, ready and the next step")
    void tracksProgress() {
        when(departments.countLiveBySchoolId(schoolId)).thenReturn(5L);
        when(lecturers.countLiveBySchoolId(schoolId)).thenReturn(55L);

        OnboardingGuideResponse.Data data = guide();

        assertThat(step(data, "DEPARTMENTS").getCompleted()).isTrue();
        assertThat(step(data, "LECTURERS").getCount()).isEqualTo(55L);
        assertThat(step(data, "ROOMS").getCompleted()).isFalse();
        assertThat(step(data, "COURSES").isReady()).isTrue();
        assertThat(step(data, "CURRICULUM").isReady()).isFalse();
        assertThat(data.getCompletedSteps()).isEqualTo(2);
        assertThat(data.getNextStep()).isEqualTo(step(data, "ROOMS").getStep());

        OnboardingGuideResponse.Step generate = step(data, OnboardingServiceImpl.GENERATE_TIMETABLE);
        assertThat(generate.getCount()).isNull();
        assertThat(generate.getCompleted()).isNull();
    }

    @Test
    @DisplayName("once every tracked step is done, the next step is generation")
    void pointsAtGenerationWhenSetUp() {
        List.of(departments, lecturers, rooms, programs, courses, programCourses, students, periods, sections,
                enrollments).forEach(repository -> when(repository.countLiveBySchoolId(schoolId)).thenReturn(1L));

        OnboardingGuideResponse.Data data = guide();

        assertThat(data.getNextStep()).isEqualTo(data.getTotalSteps());
        assertThat(data.getCompletedSteps()).isEqualTo(data.getTotalSteps() - 1);
        assertThat(step(data, OnboardingServiceImpl.GENERATE_TIMETABLE).isReady()).isTrue();
    }
}
