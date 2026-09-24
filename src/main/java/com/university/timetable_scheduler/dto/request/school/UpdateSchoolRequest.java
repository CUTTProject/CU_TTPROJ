package com.university.timetable_scheduler.dto.request.school;

import com.university.timetable_scheduler.status.SchoolEnum;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UpdateSchoolRequest {
    @NotNull(message = "id is required")
    private UUID id;
    private String schoolName;
    private String schoolAddress;
    private String schoolPhone;
    private SchoolEnum.SchoolStatus schoolStatus;
    /** Optional; a blank one keeps the stored hour. The day must still start before it ends. */
    private LocalTime schoolDayStartHour;
    private LocalTime schoolDayEndHour;
}
