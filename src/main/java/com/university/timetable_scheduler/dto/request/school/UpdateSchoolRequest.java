package com.university.timetable_scheduler.dto.request.school;

import com.university.timetable_scheduler.status.SchoolEnum;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UpdateSchoolRequest {
    @NotNull(message = "id is required")
    private UUID id;
    private String schoolName;
    private String schoolAddress;
    private String schoolPhone;
    private SchoolEnum.SchoolStatus schoolStatus;
}
