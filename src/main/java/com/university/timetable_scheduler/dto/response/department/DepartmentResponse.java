package com.university.timetable_scheduler.dto.response.department;

import java.util.UUID;

import com.university.timetable_scheduler.status.DepartmentEnum;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentResponse {
    private UUID id;
    private String departmentName;
    private DepartmentEnum.DepartmentStatus departmentStatus;
}

