package com.university.timetable_scheduler.dto.request.section;

import com.university.timetable_scheduler.status.EventEnum;
import com.university.timetable_scheduler.status.SectionEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateSectionRequest {
    @NotNull(message = "Course ID is required")
    private UUID sectionCourseId;

    @NotBlank(message = "Section name is required")
    private String sectionName;

    private String sectionEnrollmentSize;

    @NotNull(message = "Academic period ID is required")
    private UUID sectionAcademicPeriodId;

    /**
     * Optional. Total weekly time, a multiple of 5; the section's events are made from it, as in
     * the bulk upload. Without it the section has no events until they are added one by one, and
     * the fields below cannot be given.
     */
    @Min(value = 1, message = "sectionDurationMinutes must be at least 1")
    private Integer sectionDurationMinutes;

    /** Optional. The units this section is split by; without it the course's units are used. */
    @Min(value = 1, message = "sectionUnits must be at least 1")
    private Integer sectionUnits;

    /** Optional, default NO. YES splits a 3-unit section 2/3 + 1/3 and a 4-unit section in half. */
    private SectionEnum.SectionSplit sectionSplit;

    /** Optional label on the events; it does not affect generation. */
    private EventEnum.EventType sectionEventType;

    /**
     * Optional. A section in the same period this one is taught together with: this section gets
     * no events of its own and is taught in that section's events, shown as "THIS/RELATED". Pairs
     * only.
     */
    private UUID relatedToSectionId;
}
