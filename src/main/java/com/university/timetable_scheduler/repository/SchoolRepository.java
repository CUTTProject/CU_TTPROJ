package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.status.SchoolEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * School is the tenant root, so it does not extend {@link TenantAwareRepository}.
 * Callers are responsible for restricting reads to the caller's own school —
 * see SchoolServiceImpl, which pins every lookup to TenantContext.
 */
@Repository
public interface SchoolRepository extends JpaRepository<School, UUID> {

    @Query("""
        SELECT s FROM School s
        WHERE s.schoolAdminEmail = :schoolAdminEmail
          AND (s.isDeleted IS NULL OR s.isDeleted = false)
    """)
    Optional<School> findSchoolByAdminEmail(
            @Param("schoolAdminEmail") String schoolAdminEmail
    );

    @Query("""
        SELECT s FROM School s
        WHERE s.id = :schoolId
          AND (s.isDeleted IS NULL OR s.isDeleted = false)
    """)
    Optional<School> findLiveById(@Param("schoolId") UUID schoolId);

    /**
     * Filtered read, pinned to a single school. {@code schoolId} is mandatory and is applied
     * before the optional filters, so this can no longer enumerate every tenant.
     */
    @Query("""
        SELECT s FROM School s
        WHERE s.id = :schoolId
          AND (s.isDeleted IS NULL OR s.isDeleted = false)
          AND (:schoolName IS NULL OR s.schoolName = :schoolName)
          AND (:schoolStatus IS NULL OR s.schoolStatus = :schoolStatus)
    """)
    List<School> findSchoolByFilter(
            @Param("schoolId") UUID schoolId,
            @Param("schoolName") String schoolName,
            @Param("schoolStatus") SchoolEnum.SchoolStatus schoolStatus
    );
}
