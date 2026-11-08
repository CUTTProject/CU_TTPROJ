package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.academicperiod.CreateAcademicPeriodRequest;
import com.university.timetable_scheduler.dto.request.academicperiod.ReadAcademicPeriodRequest;
import com.university.timetable_scheduler.dto.request.academicperiod.UpdateAcademicPeriodRequest;
import com.university.timetable_scheduler.dto.response.academicperiod.AcademicPeriodResponse;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.generation.TimetableGenerationRunner;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.status.AcademicPeriodEnum.AcademicPeriodPhase;
import com.university.timetable_scheduler.status.AcademicPeriodEnum.AcademicPeriodStatus;
import com.university.timetable_scheduler.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * An academic period closes at its end date, or when archived: every write into it is refused,
 * and editing the period itself reopens it.
 */
@SpringBootTest
class AcademicPeriodLifecycleTest {

    @Autowired private AcademicPeriodServiceImpl academicPeriodService;
    @Autowired private SectionServiceImpl sectionService;
    @Autowired private EnrollmentServiceImpl enrollmentService;
    @Autowired private TimetableGenerationRunner generationRunner;
    @Autowired private SchoolRepository schoolRepository;
    @Autowired private Clock clock;

    private LocalDateTime now;

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "upload.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private static final String SECTION_CSV = """
            courseCode,sectionName,lecturerStaffNumbers,sectionDurationMinutes,eventType
            CSC101,CSC101-A,STF1,60,CLASS
            """;

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
        TenantContext.setSchoolId(schoolRepository.save(s).getId());
        now = LocalDateTime.now(clock);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private AcademicPeriodResponse create(String name, LocalDateTime start, LocalDateTime end) {
        return academicPeriodService.createAcademicPeriod(
                new CreateAcademicPeriodRequest(name, "2026/2027", "FIRST", start, end)).getData().getAcademicPeriod();
    }

    private static void assertConflict(Runnable call, String messagePart) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getReason()).contains(messagePart);
                });
    }

    @Test
    @DisplayName("The response carries the phase worked out from the dates")
    void phaseInResponse() {
        assertThat(create("Next", now.plusDays(10), now.plusDays(100)).getAcademicPeriodPhase())
                .isEqualTo(AcademicPeriodPhase.UPCOMING);
        assertThat(create("Current", now.minusDays(10), now.plusDays(100)).getAcademicPeriodPhase())
                .isEqualTo(AcademicPeriodPhase.IN_PROGRESS);
        assertThat(create("Last", now.minusDays(100), now.minusDays(1)).getAcademicPeriodPhase())
                .isEqualTo(AcademicPeriodPhase.ENDED);
    }

    @Test
    @DisplayName("An ended period refuses uploads, dry runs, edits and generation")
    void endedPeriodIsReadOnly() {
        UUID ended = create("Last", now.minusDays(100), now.minusDays(1)).getId();

        assertConflict(() -> sectionService.bulkUploadSections(csv(SECTION_CSV), ended, false), "ended on");
        assertConflict(() -> sectionService.bulkUploadSections(csv(SECTION_CSV), ended, true), "ended on");
        assertConflict(() -> enrollmentService.bulkUploadEnrollments(csv("""
                studentMatriculationNumber,courseCode,sectionName
                M1,CSC101,CSC101-A
                """), ended, false), "Extend its end date");
        assertConflict(() -> generationRunner.submit(ended.toString()), "ended on");
    }

    @Test
    @DisplayName("Extending the end date reopens an ended period")
    void extendingReopens() {
        UUID id = create("Last", now.minusDays(100), now.minusDays(1)).getId();

        UpdateAcademicPeriodRequest extend = new UpdateAcademicPeriodRequest();
        extend.setId(id);
        extend.setAcademicPeriodEndDate(now.plusDays(30));
        AcademicPeriodResponse updated = academicPeriodService.updateAcademicPeriod(extend).getData().getAcademicPeriod();

        assertThat(updated.getAcademicPeriodPhase()).isEqualTo(AcademicPeriodPhase.IN_PROGRESS);
        assertThatCode(() -> sectionService.bulkUploadSections(csv(SECTION_CSV), id, true)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("INACTIVE archives an open period, and ACTIVE brings it back")
    void archiveAndRestore() {
        UUID id = create("Current", now.minusDays(10), now.plusDays(100)).getId();

        UpdateAcademicPeriodRequest archive = new UpdateAcademicPeriodRequest();
        archive.setId(id);
        archive.setAcademicPeriodStatus(AcademicPeriodStatus.INACTIVE);
        assertThat(academicPeriodService.updateAcademicPeriod(archive).getData().getAcademicPeriod()
                .getAcademicPeriodPhase()).isEqualTo(AcademicPeriodPhase.ARCHIVED);
        assertConflict(() -> sectionService.bulkUploadSections(csv(SECTION_CSV), id, true), "archived");

        UpdateAcademicPeriodRequest restore = new UpdateAcademicPeriodRequest();
        restore.setId(id);
        restore.setAcademicPeriodStatus(AcademicPeriodStatus.ACTIVE);
        academicPeriodService.updateAcademicPeriod(restore);
        assertThatCode(() -> sectionService.bulkUploadSections(csv(SECTION_CSV), id, true)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("The end date must come after the start date, on create and on update")
    void endMustFollowStart() {
        assertThatThrownBy(() -> create("Backwards", now, now.minusDays(1)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        UUID id = create("Current", now.minusDays(10), now.plusDays(100)).getId();
        UpdateAcademicPeriodRequest backwards = new UpdateAcademicPeriodRequest();
        backwards.setId(id);
        backwards.setAcademicPeriodEndDate(now.minusDays(20));
        assertThatThrownBy(() -> academicPeriodService.updateAcademicPeriod(backwards))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Periods can be filtered by phase")
    void filterByPhase() {
        create("Next", now.plusDays(10), now.plusDays(100));
        create("Current", now.minusDays(10), now.plusDays(100));
        create("Last", now.minusDays(100), now.minusDays(1));

        ReadAcademicPeriodRequest read = new ReadAcademicPeriodRequest();
        read.setAcademicPeriodPhase(AcademicPeriodPhase.ENDED);
        List<AcademicPeriodResponse> endedOnly = academicPeriodService.readAcademicPeriod(read).getData().getAcademicPeriods();

        assertThat(endedOnly).extracting(AcademicPeriodResponse::getAcademicPeriodName).containsExactly("Last");
    }

    private AcademicPeriodStatus statusOf(UUID id) {
        ReadAcademicPeriodRequest read = new ReadAcademicPeriodRequest();
        read.setId(id);
        return academicPeriodService.readAcademicPeriod(read).getData().getAcademicPeriods().get(0)
                .getAcademicPeriodStatus();
    }

    @Test
    @DisplayName("Creating a period closes every other; reactivating one closes the rest")
    void onlyOneActive() {
        UUID first = create("First", now.minusDays(10), now.plusDays(100)).getId();
        UUID second = create("Second", now.minusDays(5), now.plusDays(100)).getId();

        assertThat(statusOf(first)).isEqualTo(AcademicPeriodStatus.INACTIVE);
        assertThat(statusOf(second)).isEqualTo(AcademicPeriodStatus.ACTIVE);
        assertConflict(() -> sectionService.bulkUploadSections(csv(SECTION_CSV), first, true), "archived");

        UpdateAcademicPeriodRequest restore = new UpdateAcademicPeriodRequest();
        restore.setId(first);
        restore.setAcademicPeriodStatus(AcademicPeriodStatus.ACTIVE);
        academicPeriodService.updateAcademicPeriod(restore);

        assertThat(statusOf(first)).isEqualTo(AcademicPeriodStatus.ACTIVE);
        assertThat(statusOf(second)).isEqualTo(AcademicPeriodStatus.INACTIVE);
    }
}
