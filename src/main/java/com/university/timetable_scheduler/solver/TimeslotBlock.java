package com.university.timetable_scheduler.solver;

import com.university.timetable_scheduler.entity.Timeslot;
import com.university.timetable_scheduler.status.TimeslotEnum;

import java.time.LocalTime;
import java.util.List;

/**
 * A contiguous run of {@link Timeslot}s on the same day — the "t" half of a CSP domain value.
 *
 * <p>A 2-hour event scheduled against 1-hour slots occupies a block of two back-to-back slots.
 * The block is the unit the solver reasons about, because an event must occupy all of its slots
 * or none of them.
 *
 * <p>Note on the spec: the PDF's colouring constraint is stated as {@code ti != tj}. We use
 * interval <em>overlap</em> instead, which is strictly stronger and necessary once events span
 * multiple slots — {@code ti != tj} would happily let a 9–11 block coexist with a 10–11 block.
 */
public record TimeslotBlock(List<Timeslot> timeslots) {

    public TimeslotEnum.TimeslotDay day() {
        return timeslots.get(0).getTimeslotDay();
    }

    public LocalTime startTime() {
        return timeslots.get(0).getTimeslotStartTime();
    }

    public LocalTime endTime() {
        return timeslots.get(timeslots.size() - 1).getTimeslotEndTime();
    }

    /** True when both blocks fall on the same day and their time intervals intersect. */
    public boolean overlapsWith(TimeslotBlock other) {
        if (day() != other.day()) return false;
        return startTime().isBefore(other.endTime()) && endTime().isAfter(other.startTime());
    }

    @Override
    public String toString() {
        return day() + " " + startTime() + "-" + endTime();
    }
}
