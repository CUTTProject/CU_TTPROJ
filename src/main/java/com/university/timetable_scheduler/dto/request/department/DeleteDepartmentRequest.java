package com.university.timetable_scheduler.dto.request.department;

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
public class DeleteDepartmentRequest {
    @NotNull(message = "Department ID is required")
    private UUID id;
}
