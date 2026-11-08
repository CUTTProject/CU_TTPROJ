package com.university.timetable_scheduler.status;

public class TimetableEnum {
    public enum TimetableConflictReason {
        SAME_LECTURER,
        OVERLAPPING_STUDENTS,
        UNIVERSITY_HARD_RULES
    }

    /** What is wrong with a generated timetable, one {@code TimetableConflict} row each. */
    public enum TimetableConflictType {
        /** Two events booked into the same room at overlapping times. */
        ROOM_CLASH,
        /** Two events taught by the same lecturer at overlapping times. */
        LECTURER_CLASH,
        /** Two events sharing enrolled students at overlapping times. */
        STUDENT_CLASH,
        /** Could have been placed, but the solver ran out of time before finding a clash-free spot. */
        UNASSIGNED,
        /** Could never be placed: no run of timeslots fits its duration, or no room fits its group. */
        UNSCHEDULABLE
    }
}
