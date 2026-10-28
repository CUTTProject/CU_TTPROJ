package com.university.timetable_scheduler.status;

public class TimeslotEnum {
    public enum TimeslotStatus {
        ACTIVE,
        INACTIVE
    }

    public enum TimeslotDay {
        SUNDAY,
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY
    }

    /**
     * DEFAULT slots are the system's grid, rebuilt at generation to fit the events' durations;
     * CUSTOM slots were defined by the school and are never touched. Null on rows predating this.
     */
    public enum TimeslotSource {
        DEFAULT,
        CUSTOM
    }

}
