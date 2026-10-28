package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.entity.Course;
import com.university.timetable_scheduler.entity.Event;
import com.university.timetable_scheduler.entity.Section;
import com.university.timetable_scheduler.status.SectionEnum.SectionSplit;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Turns a section's weekly duration and split setting into its events.
 *
 * <p>Splitting follows the section's units — its own when set, else its course's: 3 units become
 * 2/3 + 1/3 of the duration, 4 units 1/2 + 1/2. No other unit count splits. Every part must be a whole multiple of
 * {@link #MINUTE_STEP} minutes, so the timeslot grid built from them stays coarse enough to solve.
 */
public final class SectionEventPlanner {

    /** Durations, and so every split part, must be multiples of this. It floors the slot length. */
    public static final int MINUTE_STEP = 5;

    private SectionEventPlanner() {}

    /** The units a section is planned with: its own when the school gave them, else its course's. */
    public static Integer unitsOf(Integer sectionUnits, Course course) {
        return sectionUnits != null ? sectionUnits : course == null ? null : course.getCourseUnit();
    }

    /** Whether a section with these units can be split at all. */
    public static boolean splittable(Integer units) {
        return units != null && (units == 3 || units == 4);
    }

    /** The parts a YES split produces, in minutes. Only for {@link #splittable} units. */
    public static List<Integer> splitParts(int units, int minutes) {
        return units == 3
                ? List.of(minutes * 2 / 3, minutes / 3)
                : List.of(minutes / 2, minutes / 2);
    }

    /** Why a section cannot be planned, against the upload column at fault. */
    public record Problem(String column, String message) {}

    /**
     * Why this section cannot be planned, or null when it can. A MAYBE is checked as though it
     * were a YES, since any generation may pick that.
     */
    public static Problem problem(Integer units, int minutes, SectionSplit split) {
        if (minutes % MINUTE_STEP != 0) {
            return new Problem("sectionDurationMinutes", "Must be a multiple of " + MINUTE_STEP);
        }
        if (split == SectionSplit.YES && !splittable(units)) {
            return new Problem("split", "Only 3- and 4-unit sections can be split; this one has "
                    + (units == null ? "no units recorded" : units + " unit(s)"));
        }
        if (split != SectionSplit.NO && splittable(units)) {
            int divisor = units == 3 ? 3 : 2;
            List<Integer> parts = splitParts(units, minutes);
            if (minutes % divisor != 0 || parts.stream().anyMatch(p -> p % MINUTE_STEP != 0)) {
                return new Problem("sectionDurationMinutes", "A %d-unit section splits into %s, so the duration "
                        .formatted(units, units == 3 ? "2/3 + 1/3" : "1/2 + 1/2")
                        + "must divide into parts that are multiples of " + MINUTE_STEP
                        + " (e.g. " + (units == 3 ? 180 : 120) + ")");
            }
        }
        return null;
    }

    /** True when a MAYBE on a section with these units can never split, which is worth a warning. */
    public static boolean maybeNeverSplits(Integer units, SectionSplit split) {
        return split == SectionSplit.MAYBE && !splittable(units);
    }

    /**
     * The section's events, unsaved. A merged section gets none: its main section's events cover
     * it. MAYBE is resolved with {@code random}, so each call can differ.
     */
    public static List<Event> plan(Section section, Random random) {
        if (section.isMerged() || section.getSectionDurationMinutes() == null) {
            return List.of();
        }
        int minutes = section.getSectionDurationMinutes();
        Integer units = unitsOf(section.getSectionUnits(), section.getSectionCourse());
        boolean split = splittable(units) && switch (resolvedSplit(section)) {
            case YES -> true;
            case NO -> false;
            case MAYBE -> random.nextBoolean();
        };

        List<Integer> parts = split ? splitParts(units, minutes) : List.of(minutes);
        List<Event> events = new ArrayList<>(parts.size());
        for (int part : parts) {
            Event event = new Event();
            event.setSchool(section.getSchool());
            event.setEventSection(section);
            event.setEventDuration(Duration.ofMinutes(part));
            event.setEventType(section.getSectionEventType());
            events.add(event);
        }
        return events;
    }

    private static SectionSplit resolvedSplit(Section section) {
        return section.getSectionSplit() == null ? SectionSplit.NO : section.getSectionSplit();
    }
}
