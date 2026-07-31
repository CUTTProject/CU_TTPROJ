package com.university.timetable_scheduler.dto.request.department;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateDepartmentRequest {
    @NotBlank(message = "Department name is required")
    private String departmentName;
}
