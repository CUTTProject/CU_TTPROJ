package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.Program;
import com.university.timetable_scheduler.status.ProgramEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgramRepository extends TenantAwareRepository<Program> {

    /**
     * The programme list behind the table: filtered, newest first, one page at a time.
     *
     * <p>Department and coordinator are fetch-joined because every row renders their names;
     * without it a page of 20 costs 41 queries. Both are {@code @ManyToOne}, so the fetch
     * join is safe to paginate (Hibernate only has to buffer in memory for collection fetches).
     * The count query deliberately omits the joins and the ordering.
     *
     * <p>{@code programName} matches as a case-insensitive substring — it backs the search
     * box rather than an exact lookup.
     */
    @Query(value = """
        SELECT p FROM Program p
        LEFT JOIN FETCH p.programDepartment d
        LEFT JOIN FETCH p.programCoordinator c
        WHERE p.school.id = :schoolId
          AND (p.isDeleted IS NULL OR p.isDeleted = false)
          AND (:id IS NULL OR p.id = :id)
          AND (CAST(:programName AS String) IS NULL
               OR UPPER(p.programName) LIKE UPPER(CONCAT('%', CAST(:programName AS String), '%')))
          AND (:programDepartmentId IS NULL OR d.id = :programDepartmentId)
          AND (:programLevel IS NULL OR p.programLevel = :programLevel)
          AND (:programStatus IS NULL OR p.programStatus = :programStatus)
        ORDER BY p.createdAt DESC
    """,
            countQuery = """
        SELECT COUNT(p) FROM Program p
        WHERE p.school.id = :schoolId
          AND (p.isDeleted IS NULL OR p.isDeleted = false)
          AND (:id IS NULL OR p.id = :id)
          AND (CAST(:programName AS String) IS NULL
               OR UPPER(p.programName) LIKE UPPER(CONCAT('%', CAST(:programName AS String), '%')))
          AND (:programDepartmentId IS NULL OR p.programDepartment.id = :programDepartmentId)
          AND (:programLevel IS NULL OR p.programLevel = :programLevel)
          AND (:programStatus IS NULL OR p.programStatus = :programStatus)
    """)
    Page<Program> findProgramByFilter(
            @Param("schoolId") UUID schoolId,
            @Param("id") UUID id,
            @Param("programName") String programName,
            @Param("programDepartmentId") UUID programDepartmentId,
            @Param("programLevel") ProgramEnum.ProgramLevel programLevel,
            @Param("programStatus") ProgramEnum.ProgramStatus programStatus,
            Pageable pageable
    );

    /**
     * True if another live programme in the same department already carries this name.
     * Programmes have no code of their own, so name-within-department is the identity.
     */
    @Query("""
        SELECT COUNT(p) > 0 FROM Program p
        WHERE p.school.id = :schoolId
          AND (p.isDeleted IS NULL OR p.isDeleted = false)
          AND p.programDepartment.id = :programDepartmentId
          AND UPPER(TRIM(p.programName)) = UPPER(TRIM(:programName))
          AND (:excludeId IS NULL OR p.id <> :excludeId)
    """)
    boolean existsLiveByProgramNameInDepartment(
            @Param("schoolId") UUID schoolId,
            @Param("programName") String programName,
            @Param("programDepartmentId") UUID programDepartmentId,
            @Param("excludeId") UUID excludeId
    );

    /** The same identity rule as above, used by the bulk upload to decide create vs update. */
    @Query("""
        SELECT p FROM Program p
        WHERE p.school.id = :schoolId
          AND (p.isDeleted IS NULL OR p.isDeleted = false)
          AND p.programDepartment.id = :programDepartmentId
          AND UPPER(TRIM(p.programName)) = UPPER(TRIM(:programName))
    """)
    Optional<Program> findLiveByProgramNameInDepartment(
            @Param("schoolId") UUID schoolId,
            @Param("programName") String programName,
            @Param("programDepartmentId") UUID programDepartmentId
    );

    /** Live programmes in one status, for the stat cards. */
    @Query("""
        SELECT COUNT(p) FROM Program p
        WHERE p.school.id = :schoolId
          AND (p.isDeleted IS NULL OR p.isDeleted = false)
          AND p.programStatus = :programStatus
    """)
    long countLiveByStatus(
            @Param("schoolId") UUID schoolId,
            @Param("programStatus") ProgramEnum.ProgramStatus programStatus
    );
}
