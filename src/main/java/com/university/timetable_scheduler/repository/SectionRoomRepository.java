package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.SectionRoom;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SectionRoomRepository extends TenantAwareRepository<SectionRoom> {

    /** All room restrictions for a given section, scoped to the owning school. */
    @Query("""
        SELECT sr FROM SectionRoom sr
        WHERE sr.sectionRoomSection.id = :sectionId
          AND sr.school.id = :schoolId
          AND (sr.isDeleted IS NULL OR sr.isDeleted = false)
    """)
    List<SectionRoom> findAllBySectionRoomSection_Id(
            @Param("sectionId") UUID sectionId,
            @Param("schoolId") UUID schoolId);

    /**
     * Every live link of every section in a period, in one query. A bulk section upload replaces
     * each section's links, and fetching them per section was one query per row.
     */
    @Query("""
        SELECT sr FROM SectionRoom sr
        WHERE sr.school.id = :schoolId
          AND (sr.isDeleted IS NULL OR sr.isDeleted = false)
          AND sr.sectionRoomSection.sectionAcademicPeriod.id = :academicPeriodId
    """)
    List<SectionRoom> findAllLiveByAcademicPeriod(
            @Param("schoolId") UUID schoolId,
            @Param("academicPeriodId") UUID academicPeriodId);
}
