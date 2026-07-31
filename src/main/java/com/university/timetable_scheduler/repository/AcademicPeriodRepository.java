package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.AcademicPeriod;
import com.university.timetable_scheduler.status.AcademicPeriodEnum;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AcademicPeriodRepository extends TenantAwareRepository<AcademicPeriod> {

    @Query("""
        SELECT a FROM AcademicPeriod a
        WHERE a.school.id = :schoolId
          AND (a.isDeleted IS NULL OR a.isDeleted = false)
          AND (:id IS NULL OR a.id = :id)
          AND (:academicPeriodName IS NULL OR a.academicPeriodName = :academicPeriodName)
          AND (:academicPeriodSession IS NULL OR a.academicPeriodSession = :academicPeriodSession)
          AND (:academicPeriodSemester IS NULL OR a.academicPeriodSemester = :academicPeriodSemester)
          AND (:academicPeriodStatus IS NULL OR a.academicPeriodStatus = :academicPeriodStatus)
    """)
    List<AcademicPeriod> findAcademicPeriodByFilter(
            @Param("schoolId") UUID schoolId,
            @Param("id") UUID id,
            @Param("academicPeriodName") String academicPeriodName,
            @Param("academicPeriodSession") String academicPeriodSession,
            @Param("academicPeriodSemester") String academicPeriodSemester,
            @Param("academicPeriodStatus") AcademicPeriodEnum.AcademicPeriodStatus academicPeriodStatus
    );
}
