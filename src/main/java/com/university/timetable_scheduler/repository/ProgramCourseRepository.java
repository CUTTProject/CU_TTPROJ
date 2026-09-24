package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.ProgramCourse;
import com.university.timetable_scheduler.status.CourseEnum;
import com.university.timetable_scheduler.status.ProgramCourseEnum;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProgramCourseRepository extends TenantAwareRepository<ProgramCourse> {
    @Query("""
        SELECT pc FROM ProgramCourse pc
        WHERE pc.school.id = :schoolId
          AND (pc.isDeleted IS NULL OR pc.isDeleted = false)
          AND (:programId IS NULL OR pc.programCourseProgram.id = :programId)
          AND (:courseId IS NULL OR pc.programCourseCourse.id = :courseId)
          AND (:level IS NULL OR pc.programCourseLevel = :level)
          AND (:semester IS NULL OR pc.programCourseSemester = :semester)
        ORDER BY pc.programCourseLevel, pc.programCourseSemester
    """)
    List<ProgramCourse> findProgramCourseByFilter(
            @Param("schoolId") UUID schoolId,
            @Param("programId") UUID programId,
            @Param("courseId") UUID courseId,
            @Param("level") CourseEnum.CourseLevel level,
            @Param("semester") ProgramCourseEnum.Semester semester
    );

    @Query("""
        SELECT COUNT(pc) > 0 FROM ProgramCourse pc
        WHERE pc.school.id = :schoolId
          AND (pc.isDeleted IS NULL OR pc.isDeleted = false)
          AND pc.programCourseProgram.id = :programId
          AND pc.programCourseCourse.id = :courseId
    """)
    boolean existsLive(
            @Param("schoolId") UUID schoolId,
            @Param("programId") UUID programId,
            @Param("courseId") UUID courseId
    );
}
