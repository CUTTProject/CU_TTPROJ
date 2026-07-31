package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.Timeslot;
import com.university.timetable_scheduler.status.TimeslotEnum;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimeslotRepository extends TenantAwareRepository<Timeslot> {

    @Query("""
        SELECT COUNT(t) FROM Timeslot t
        WHERE t.school.id = :schoolId
          AND (t.isDeleted IS NULL OR t.isDeleted = false)
    """)
    long countBySchool_Id(@Param("schoolId") UUID schoolId);

    @Query("""
        SELECT t FROM Timeslot t
        WHERE t.school.id = :schoolId
          AND (t.isDeleted IS NULL OR t.isDeleted = false)
          AND (:id IS NULL OR t.id = :id)
          AND (:timeslotDay IS NULL OR t.timeslotDay = :timeslotDay)
          AND (:timeslotStatus IS NULL OR t.timeslotStatus = :timeslotStatus)
    """)
    List<Timeslot> findTimeslotByFilter(
            @Param("schoolId") UUID schoolId,
            @Param("id") UUID id,
            @Param("timeslotDay") TimeslotEnum.TimeslotDay timeslotDay,
            @Param("timeslotStatus") TimeslotEnum.TimeslotStatus timeslotStatus
    );
}
