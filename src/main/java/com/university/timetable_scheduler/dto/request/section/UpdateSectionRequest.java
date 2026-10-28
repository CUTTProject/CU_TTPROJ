package com.university.timetable_scheduler.dto.request.section;

import com.university.timetable_scheduler.status.EventEnum;
import com.university.timetable_scheduler.status.SectionEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class UpdateSectionRequest {
    @NotNull(message = "Section ID is required")
    private UUID id;

    private UUID sectionCourseId;
    private String sectionName;
    private String sectionEnrollmentSize;
    private UUID sectionAcademicPeriodId;

    /** Optional, as on create. Changing any of these re-plans the section's events. */
    @Min(value = 1, message = "sectionDurationMinutes must be at least 1")
    private Integer sectionDurationMinutes;
    @Min(value = 1, message = "sectionUnits must be at least 1")
    private Integer sectionUnits;
    private SectionEnum.SectionSplit sectionSplit;
    private EventEnum.EventType sectionEventType;

    /**
     * Optional. Teaches this section as part of that section, in the same period, replacing any
     * section it was taught with before. Cannot be given with {@link #removeRelatedTo}.
     */
    private UUID relatedToSectionId;

    /** Optional. True stops this section being taught as part of another; it gets its own events back. */
    private Boolean removeRelatedTo;
}
