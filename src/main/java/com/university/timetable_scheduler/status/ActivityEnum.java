package com.university.timetable_scheduler.status;

public class ActivityEnum {
    /** What happened. The frontend picks the icon and colour from this, not from the title. */
    public enum ActivityType {
        ACADEMIC_PERIOD_CREATED,
        COURSE_CREATED,
        COURSES_UPLOADED,
        CURRICULUM_UPLOADED,
        DEPARTMENTS_UPLOADED,
        ENROLLMENTS_UPLOADED,
        LECTURER_CREATED,
        LECTURERS_UPLOADED,
        PROGRAM_CREATED,
        PROGRAMS_UPLOADED,
        ROOM_CREATED,
        ROOMS_UPLOADED,
        SECTIONS_UPLOADED,
        STUDENT_CREATED,
        STUDENTS_IMPORTED,
        TIMETABLE_UPLOADED,
        TIMETABLE_GENERATED,
        TIMETABLE_GENERATION_FAILED
    }
}
