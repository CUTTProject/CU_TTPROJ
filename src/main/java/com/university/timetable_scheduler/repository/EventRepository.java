package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.Event;
import com.university.timetable_scheduler.status.EventEnum;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends TenantAwareRepository<Event> {

    @Query("""
        SELECT e FROM Event e
        WHERE e.school.id = :schoolId
          AND (e.isDeleted IS NULL OR e.isDeleted = false)
          AND e.eventSection.sectionAcademicPeriod.id = :academicPeriodId
    """)
    List<Event> findAllBySchoolIdAndAcademicPeriodId(
            @Param("schoolId") UUID schoolId,
            @Param("academicPeriodId") UUID academicPeriodId);

    @Query("""
        SELECT e FROM Event e
        WHERE e.school.id = :schoolId
          AND (e.isDeleted IS NULL OR e.isDeleted = false)
          AND (:id IS NULL OR e.id = :id)
          AND (:sectionId IS NULL OR e.eventSection.id = :sectionId)
          AND (:eventType IS NULL OR e.eventType = :eventType)
          AND (:eventStatus IS NULL OR e.eventStatus = :eventStatus)
    """)
    List<Event> findEventByFilter(
            @Param("schoolId") UUID schoolId,
            @Param("id") UUID id,
            @Param("sectionId") UUID sectionId,
            @Param("eventType") EventEnum.EventType eventType,
            @Param("eventStatus") EventEnum.EventStatus eventStatus
    );
}
