package com.university.timetable_scheduler.dto.request.section;

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
public class DeleteSectionRequest {
    @NotNull(message = "Section ID is required")
    private UUID id;
}
