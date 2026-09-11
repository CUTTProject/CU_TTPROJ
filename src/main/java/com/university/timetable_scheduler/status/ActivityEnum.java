package com.university.timetable_scheduler.status;

public class ActivityEnum {
    /** What happened. The frontend picks the icon and colour from this, not from the title. */
    public enum ActivityType {
        ACADEMIC_PERIOD_CREATED,
        COURSE_CREATED,
        LECTURER_CREATED,
        ROOM_CREATED,
        ROOMS_UPLOADED,
        STUDENT_CREATED,
        STUDENTS_IMPORTED,
        TIMETABLE_UPLOADED,
        TIMETABLE_GENERATED,
        TIMETABLE_GENERATION_FAILED
    }
}
