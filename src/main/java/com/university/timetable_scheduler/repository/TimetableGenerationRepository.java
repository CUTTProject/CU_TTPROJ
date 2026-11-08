package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.TimetableGeneration;
import com.university.timetable_scheduler.status.JobEnum;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TimetableGenerationRepository extends TenantAwareRepository<TimetableGeneration> {

    /** The most recent run with this outcome for one period. */
    Optional<TimetableGeneration> findFirstBySchool_IdAndAcademicPeriod_IdAndGenerationStatusAndIsDeletedFalseOrderByFinishedAtDesc(
            UUID schoolId, UUID academicPeriodId, JobEnum.JobStatus generationStatus);

    Optional<TimetableGeneration> findFirstBySchool_IdAndJobIdAndIsDeletedFalse(UUID schoolId, UUID jobId);
}
