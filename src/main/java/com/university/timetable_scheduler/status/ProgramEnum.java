package com.university.timetable_scheduler.status;

public class ProgramEnum {
    public enum ProgramStatus {
        ACTIVE,
        INACTIVE
    }

    /**
     * The academic tier a programme sits in. Deliberately not reusing
     * {@code StudentEnum.StudentLevel} / {@code CourseEnum.CourseLevel}: those mean the
     * year a student is in (LEVEL_100 ...), which is a different axis entirely.
     */
    public enum ProgramLevel {
        UNDERGRADUATE,
        POSTGRADUATE
    }
}
