package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.JobEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One finished generation run. Kept for every run, so the latest is a lookup and earlier runs stay
 * comparable; its conflicts are {@link TimetableConflict} rows.
 *
 * <p>Only finished runs are written. A {@code SUCCEEDED} row is written in the same transaction as
 * the assignment, so the latest one always describes the timetable on the Event rows.
 */
@Entity
@Table(indexes = {
        @Index(name = "idx_timetable_generation_period", columnList = "schoolId, academicPeriodId, finishedAt"),
        @Index(name = "idx_timetable_generation_job", columnList = "jobId")
})
@NoArgsConstructor
@Getter
@Setter
public class TimetableGeneration extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academicPeriodId", nullable = false)
    private AcademicPeriod academicPeriod;

    private UUID jobId;

    /** SUCCEEDED or FAILED. */
    @Enumerated(EnumType.STRING)
    private JobEnum.JobStatus generationStatus;

    private Instant submittedAt;
    private Instant startedAt;
    private Instant finishedAt;

    private Boolean feasible;
    private String stopReason;
    private String failureMessage;

    private Integer totalEvents;
    private Integer scheduledEvents;

    private Integer roomClashes;
    private Integer lecturerClashes;
    private Integer studentClashes;
    private Integer unassignedEvents;
    private Integer unschedulableEvents;
}
