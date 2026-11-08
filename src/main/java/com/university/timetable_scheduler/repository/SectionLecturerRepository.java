package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.SectionLecturer;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SectionLecturerRepository extends TenantAwareRepository<SectionLecturer> {

    @Query("""
        SELECT sl FROM SectionLecturer sl
        WHERE sl.school.id = :schoolId
          AND (sl.isDeleted IS NULL OR sl.isDeleted = false)
          AND (:id IS NULL OR sl.id = :id)
          AND (:sectionId IS NULL OR sl.sectionLecturerSection.id = :sectionId)
          AND (:lecturerId IS NULL OR sl.sectionLecturerLecturer.id = :lecturerId)
    """)
    List<SectionLecturer> findSectionLecturerByFilter(
            @Param("schoolId") UUID schoolId,
            @Param("id") UUID id,
            @Param("sectionId") UUID sectionId,
            @Param("lecturerId") UUID lecturerId
    );

    /**
     * Every live link of every section in a period, in one query. A bulk section upload replaces
     * each section's links, and fetching them per section was one query per row.
     */
    @Query("""
        SELECT sl FROM SectionLecturer sl
        WHERE sl.school.id = :schoolId
          AND (sl.isDeleted IS NULL OR sl.isDeleted = false)
          AND sl.sectionLecturerSection.sectionAcademicPeriod.id = :academicPeriodId
    """)
    List<SectionLecturer> findAllLiveByAcademicPeriod(
            @Param("schoolId") UUID schoolId,
            @Param("academicPeriodId") UUID academicPeriodId);
}
