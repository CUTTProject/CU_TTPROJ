package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.TimetableConflict;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimetableConflictRepository extends TenantAwareRepository<TimetableConflict> {

    List<TimetableConflict> findAllBySchool_IdAndGeneration_IdAndIsDeletedFalse(UUID schoolId, UUID generationId);
}
