package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.SectionTimeslot;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SectionTimeslotRepository extends TenantAwareRepository<SectionTimeslot> {

    @Query("""
        SELECT st FROM SectionTimeslot st
        WHERE st.school.id = :schoolId
          AND (st.isDeleted IS NULL OR st.isDeleted = false)
          AND (:id IS NULL OR st.id = :id)
          AND (:sectionId IS NULL OR st.sectionTimeslotSection.id = :sectionId)
          AND (:timeslotId IS NULL OR st.sectionTimeslotTimeslot.id = :timeslotId)
    """)
    List<SectionTimeslot> findSectionTimeslotByFilter(
            @Param("schoolId") UUID schoolId,
            @Param("id") UUID id,
            @Param("sectionId") UUID sectionId,
            @Param("timeslotId") UUID timeslotId
    );
}
