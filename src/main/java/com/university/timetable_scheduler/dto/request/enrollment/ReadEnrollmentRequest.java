package com.university.timetable_scheduler.dto.request.enrollment;

import com.university.timetable_scheduler.status.EnrollmentEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReadEnrollmentRequest {
    private UUID id;
    private UUID enrollmentStudentId;
    private UUID enrollmentSectionId;
    private UUID enrollmentAcademicPeriodId;
    private EnrollmentEnum.EnrollmentStatus enrollmentStatus;
}
