package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.Student;
import com.university.timetable_scheduler.status.StudentEnum;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
