package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.CourseEnum;
import com.university.timetable_scheduler.status.ProgramCourseEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One line of a programme's curriculum: the programme takes this course at this level, in this
 * semester, as a core course or an elective. Unique per programme and course among live rows.
 *
 * <p>Reference data only. Timetable generation does not read it; clashes still come from shared
 * lecturers and shared enrolled students.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProgramCourse extends TenantAwareEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "programCourseProgramId", nullable = false)
    private Program programCourseProgram;

    @ManyToOne(optional = false)
    @JoinColumn(name = "programCourseCourseId", nullable = false)
    private Course programCourseCourse;

    @Enumerated(EnumType.STRING)
    private CourseEnum.CourseLevel programCourseLevel;

    @Enumerated(EnumType.STRING)
    private ProgramCourseEnum.Semester programCourseSemester;

    private Boolean programCourseIsCore;
}
