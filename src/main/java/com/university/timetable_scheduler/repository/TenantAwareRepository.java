package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.TenantAwareEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Base repository for every {@link TenantAwareEntity}.
 *
 * <p>{@code JpaRepository.findById} is inherited and is NOT tenant-scoped: it will happily
 * return another school's row. Service code must never call it on a tenant-aware entity —
 * use {@link #findByIdAndSchoolId} instead, which requires the tenant to be named explicitly
 * and so cannot silently fail open.
 *
 * <p>Hibernate's {@code @Filter} is deliberately not used here: it is not applied to
 * {@code Session.find()} / {@code findById}, which is precisely the hole that needs closing.
 *
 * <p>The {@code isDeleted IS NULL} half of the soft-delete predicate tolerates rows written
 * before {@code isDeleted} was defaulted; see docs/REFACTOR.md.
 */
@NoRepositoryBean
public interface TenantAwareRepository<T extends TenantAwareEntity> extends JpaRepository<T, UUID> {

    /** Tenant-scoped, soft-delete-aware replacement for {@code findById}. */
    @Query("""
        SELECT e FROM #{#entityName} e
        WHERE e.id = :id
          AND e.school.id = :schoolId
          AND (e.isDeleted IS NULL OR e.isDeleted = false)
    """)
    Optional<T> findByIdAndSchoolId(@Param("id") UUID id, @Param("schoolId") UUID schoolId);

    /** All live rows belonging to one school. */
    @Query("""
        SELECT e FROM #{#entityName} e
        WHERE e.school.id = :schoolId
          AND (e.isDeleted IS NULL OR e.isDeleted = false)
    """)
    List<T> findAllBySchool_Id(@Param("schoolId") UUID schoolId);
}
