package com.university.timetable_scheduler.dto.request.timetable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class TimetableEvaluation
{
    /** Events with no valid assignment */
    private Integer unassignedEventConflictScore = 0;

    /** Pairs of events in the same room whose time blocks overlap (partial or identical) */
    private Integer partialCompleteDomainConflictScore = 0;

    /** Pairs of events that share a lecturer or students and are scheduled at overlapping times */
    private Integer lecturerStudentConflictScore = 0;

    public Integer getTotalConflictScore() {
        return unassignedEventConflictScore + partialCompleteDomainConflictScore + lecturerStudentConflictScore;
    }

    private Set<UUID> conflictingEvents = new HashSet<>();
}
