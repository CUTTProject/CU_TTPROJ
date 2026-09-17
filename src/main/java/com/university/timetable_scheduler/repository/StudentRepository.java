package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.Student;
import com.university.timetable_scheduler.status.StudentEnum;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends TenantAwareRepository<Student> {

    @Query("""
        SELECT s FROM Student s
        WHERE s.school.id = :schoolId
          AND (s.isDeleted IS NULL OR s.isDeleted = false)
          AND (:id IS NULL OR s.id = :id)
          AND (:studentFirstName IS NULL OR s.studentFirstName = :studentFirstName)
          AND (:studentLastName IS NULL OR s.studentLastName = :studentLastName)
          AND (:studentMatriculationNumber IS NULL OR s.studentMatriculationNumber = :studentMatriculationNumber)
          AND (:studentEmail IS NULL OR s.studentEmail = :studentEmail)
          AND (:studentLevel IS NULL OR s.studentLevel = :studentLevel)
          AND (:departmentId IS NULL OR s.studentDepartment.id = :departmentId)
          AND (:programId IS NULL OR s.studentProgram.id = :programId)
          AND (:studentStatus IS NULL OR s.studentStatus = :studentStatus)
    """)
    List<Student> findStudentByFilter(
            @Param("schoolId") UUID schoolId,
            @Param("id") UUID id,
            @Param("studentFirstName") String studentFirstName,
            @Param("studentLastName") String studentLastName,
            @Param("studentMatriculationNumber") String studentMatriculationNumber,
            @Param("studentEmail") String studentEmail,
            @Param("studentLevel") StudentEnum.StudentLevel studentLevel,
            @Param("departmentId") UUID departmentId,
            @Param("programId") UUID programId,
            @Param("studentStatus") StudentEnum.StudentStatus studentStatus
    );

    /**
     * Matriculation numbers are only unique within a school, so this lookup must be scoped.
     * Unscoped, a bulk import could attach another school's Student to this school's Enrollment.
     */
    @Query("""
        SELECT s FROM Student s
        WHERE s.studentMatriculationNumber = :studentMatriculationNumber
          AND s.school.id = :schoolId
          AND (s.isDeleted IS NULL OR s.isDeleted = false)
    """)
    Optional<Student> findByMatriculationNumberForTenant(
            @Param("studentMatriculationNumber") String studentMatriculationNumber,
            @Param("schoolId") UUID schoolId);

    /**
     * Live student headcount for several programmes at once. The programme list renders a
     * count per row, and one query per row would put the page's cost at O(rows); this keeps
     * it at one. Callers must not pass an empty collection - {@code IN ()} is not valid SQL.
     */
    @Query("""
        SELECT s.studentProgram.id AS programId, COUNT(s) AS studentCount FROM Student s
        WHERE s.school.id = :schoolId
          AND (s.isDeleted IS NULL OR s.isDeleted = false)
          AND s.studentProgram.id IN :programIds
        GROUP BY s.studentProgram.id
    """)
    List<ProgramStudentCount> countLiveByProgramIds(
            @Param("schoolId") UUID schoolId,
            @Param("programIds") Collection<UUID> programIds);

    /** Projection for {@link #countLiveByProgramIds}. */
    interface ProgramStudentCount {
        UUID getProgramId();
        long getStudentCount();
    }
}
