package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.Activity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityRepository extends TenantAwareRepository<Activity> {

    /** Newest first; the page caps how many rows are read. */
    @Query("""
        SELECT a FROM Activity a
        WHERE a.school.id = :schoolId
          AND (a.isDeleted IS NULL OR a.isDeleted = false)
        ORDER BY a.activityOccurredAt DESC
    """)
    List<Activity> findRecent(@Param("schoolId") UUID schoolId, Pageable pageable);
}
