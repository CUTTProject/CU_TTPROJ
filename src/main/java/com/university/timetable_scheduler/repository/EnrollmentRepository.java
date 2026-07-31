package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.Enrollment;
import com.university.timetable_scheduler.status.EnrollmentEnum;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends TenantAwareRepository<Enrollment> {

    @Query("""
        SELECT e FROM Enrollment e
        WHERE e.school.id = :schoolId
          AND (e.isDeleted IS NULL OR e.isDeleted = false)
          AND (:id IS NULL OR e.id = :id)
          AND (:studentId IS NULL OR e.enrollmentStudent.id = :studentId)
          AND (:sectionId IS NULL OR e.enrollmentSection.id = :sectionId)
          AND (:enrollmentStatus IS NULL OR e.enrollmentStatus = :enrollmentStatus)
    """)
    List<Enrollment> findEnrollmentByFilter(
            @Param("schoolId") UUID schoolId,
            @Param("id") UUID id,
            @Param("studentId") UUID studentId,
            @Param("sectionId") UUID sectionId,
            @Param("enrollmentStatus") EnrollmentEnum.EnrollmentStatus enrollmentStatus
    );

    /**
     * Duplicate-enrollment check. Scoped to the school: the previous derived query matched on
     * student+section alone, so another tenant's enrollment could suppress a legitimate insert.
     */
    @Query("""
        SELECT COUNT(e) > 0 FROM Enrollment e
        WHERE e.school.id = :schoolId
          AND e.enrollmentStudent.id = :studentId
          AND e.enrollmentSection.id = :sectionId
          AND (e.isDeleted IS NULL OR e.isDeleted = false)
    """)
    boolean existsForTenant(
            @Param("schoolId") UUID schoolId,
            @Param("studentId") UUID studentId,
            @Param("sectionId") UUID sectionId
    );
}
