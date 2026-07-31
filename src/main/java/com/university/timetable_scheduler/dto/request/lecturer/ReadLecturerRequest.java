package com.university.timetable_scheduler.dto.request.lecturer;

import java.util.UUID;

import com.university.timetable_scheduler.status.LecturerEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReadLecturerRequest {
    private UUID id;
    private String lecturerStaffNumber;
    private String lecturerFirstName;
    private String lecturerLastName;
    private String lecturerEmail;
    private UUID lecturerDepartmentId;
    private LecturerEnum.LecturerStatus lecturerStatus;
}

