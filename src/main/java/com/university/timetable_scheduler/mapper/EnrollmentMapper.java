package com.university.timetable_scheduler.mapper;

import com.university.timetable_scheduler.dto.response.enrollment.EnrollmentResponse;
import com.university.timetable_scheduler.entity.Enrollment;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EnrollmentMapper {
    @Mapping(source = "enrollmentStudent.id", target = "enrollmentStudentId")
    @Mapping(source = "enrollmentSection.id", target = "enrollmentSectionId")
    EnrollmentResponse toResponse(Enrollment enrollment);

    List<EnrollmentResponse> toResponseList(List<Enrollment> enrollments);
}

