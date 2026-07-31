package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.Section;
import com.university.timetable_scheduler.status.SectionEnum;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SectionRepository extends TenantAwareRepository<Section> {

    @Query("""
        SELECT s FROM Section s
        WHERE s.school.id = :schoolId
          AND (s.isDeleted IS NULL OR s.isDeleted = false)
          AND (:id IS NULL OR s.id = :id)
          AND (:courseId IS NULL OR s.sectionCourse.id = :courseId)
          AND (:sectionName IS NULL OR s.sectionName = :sectionName)
          AND (:academicPeriodId IS NULL OR s.sectionAcademicPeriod.id = :academicPeriodId)
          AND (:sectionStatus IS NULL OR s.sectionStatus = :sectionStatus)
    """)
    List<Section> findSectionByFilter(
            @Param("schoolId") UUID schoolId,
            @Param("id") UUID id,
            @Param("courseId") UUID courseId,
            @Param("sectionName") String sectionName,
            @Param("academicPeriodId") UUID academicPeriodId,
            @Param("sectionStatus") SectionEnum.SectionStatus sectionStatus
    );

    /**
     * Sections sharing a lecturer with the given section. Scoped to the school: without the
     * school predicate this joined SectionLecturer across every tenant, so another school's
     * sections were fed into this school's conflict detection.
     */
    @Query("""
        SELECT DISTINCT s2 FROM Section s2
        JOIN SectionLecturer sl2 ON s2.id = sl2.sectionLecturerSection.id
        WHERE s2.school.id = :schoolId
          AND (s2.isDeleted IS NULL OR s2.isDeleted = false)
          AND sl2.sectionLecturerLecturer.id IN (
              SELECT sl1.sectionLecturerLecturer.id FROM SectionLecturer sl1
              WHERE sl1.sectionLecturerSection.id = :sectionId
                AND sl1.school.id = :schoolId
          )
          AND s2.id <> :sectionId
    """)
    List<Section> findIntersectingSectionByLecturer(
            @Param("schoolId") UUID schoolId,
            @Param("sectionId") UUID sectionId);

    /** Sections sharing a student with the given section. Scoped to the school; see above. */
    @Query("""
        SELECT DISTINCT s2 FROM Section s2
        JOIN Enrollment e2 ON s2.id = e2.enrollmentSection.id
        WHERE s2.school.id = :schoolId
          AND (s2.isDeleted IS NULL OR s2.isDeleted = false)
          AND e2.enrollmentStudent.id IN (
              SELECT e1.enrollmentStudent.id FROM Enrollment e1
              WHERE e1.enrollmentSection.id = :sectionId
                AND e1.school.id = :schoolId
          )
          AND s2.id <> :sectionId
    """)
    List<Section> findIntersectingSectionByEnrollment(
            @Param("schoolId") UUID schoolId,
            @Param("sectionId") UUID sectionId);

    @Query("""
        SELECT s FROM Section s
        WHERE s.sectionName = :sectionName
          AND s.school.id = :schoolId
          AND (s.isDeleted IS NULL OR s.isDeleted = false)
    """)
    Optional<Section> findBySectionNameAndSchool_Id(
            @Param("sectionName") String sectionName,
            @Param("schoolId") UUID schoolId);

    record SectionTimeslotCount(Section section, Long count) {}

    @Query("""
        SELECT s, COUNT(st) FROM Section s
        JOIN SectionTimeslot st ON (s.id = st.sectionTimeslotSection.id)
        WHERE s.school.id = :schoolId
          AND (s.isDeleted IS NULL OR s.isDeleted = false)
          AND (st.isDeleted IS NULL OR st.isDeleted = false)
        GROUP BY s
        ORDER BY COUNT(st) ASC
    """)
    List<SectionTimeslotCount> orderSectionsByAvailableTimeslotCount(@Param("schoolId") UUID schoolId);
}
