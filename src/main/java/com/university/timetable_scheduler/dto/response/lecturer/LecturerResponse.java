package com.university.timetable_scheduler.dto.response.lecturer;

import java.util.UUID;

import com.university.timetable_scheduler.status.LecturerEnum;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LecturerResponse {
    private UUID id;
    private String lecturerStaffNumber;
    private String lecturerFirstName;
    private String lecturerLastName;
    private String lecturerEmail;
    private UUID lecturerDepartmentId;
    private LecturerEnum.LecturerStatus lecturerStatus;
}

