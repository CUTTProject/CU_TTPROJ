package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.Course;
import com.university.timetable_scheduler.status.CourseEnum;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseRepository extends TenantAwareRepository<Course> {

    @Query("""
        SELECT c FROM Course c
        WHERE c.school.id = :schoolId
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (:id IS NULL OR c.id = :id)
          AND (:courseCode IS NULL OR c.courseCode = :courseCode)
          AND (:courseName IS NULL OR c.courseName = :courseName)
          AND (:courseUnit IS NULL OR c.courseUnit = :courseUnit)
          AND (:courseLevel IS NULL OR c.courseLevel = :courseLevel)
          AND (:courseStatus IS NULL OR c.courseStatus = :courseStatus)
    """)
    List<Course> findCourseByFilter(
            @Param("schoolId") UUID schoolId,
            @Param("id") UUID id,
            @Param("courseCode") String courseCode,
            @Param("courseName") String courseName,
            @Param("courseUnit") Integer courseUnit,
            @Param("courseLevel") CourseEnum.CourseLevel courseLevel,
            @Param("courseStatus") CourseEnum.CourseStatus courseStatus
    );
}
