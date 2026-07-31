package com.university.timetable_scheduler.dto.request.department;

import com.university.timetable_scheduler.status.DepartmentEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReadDepartmentRequest {
    private UUID id;
    private String departmentName;
    private DepartmentEnum.DepartmentStatus departmentStatus;
}

