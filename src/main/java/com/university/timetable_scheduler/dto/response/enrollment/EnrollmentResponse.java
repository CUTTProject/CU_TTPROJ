package com.university.timetable_scheduler.dto.response.enrollment;

import java.util.UUID;

import com.university.timetable_scheduler.status.EnrollmentEnum;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnrollmentResponse {
    private UUID id;
    private UUID enrollmentStudentId;
    private UUID enrollmentSectionId;
    private EnrollmentEnum.EnrollmentStatus enrollmentStatus;
}

