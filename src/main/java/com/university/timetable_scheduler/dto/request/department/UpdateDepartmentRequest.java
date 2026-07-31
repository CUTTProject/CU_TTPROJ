package com.university.timetable_scheduler.dto.request.department;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class UpdateDepartmentRequest {
    @NotNull(message = "Department ID is required")
    private UUID id;

    private String departmentName;
}
