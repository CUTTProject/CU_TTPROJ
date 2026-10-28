package com.university.timetable_scheduler.status;

public class SectionEnum {
    public enum SectionStatus {
        ACTIVE,
        INACTIVE
    }

    /**
     * Whether a section's weekly time is split across events. YES splits a 3-unit course 2/3 + 1/3
     * and a 4-unit course 1/2 + 1/2; MAYBE picks YES or NO afresh on every generation.
     */
    public enum SectionSplit {
        YES,
        NO,
        MAYBE
    }
}
