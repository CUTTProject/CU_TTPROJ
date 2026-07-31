package com.university.timetable_scheduler.dto.request.school;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DeleteSchoolRequest {
    @NotNull(message = "id is required")
    private UUID id;
}
