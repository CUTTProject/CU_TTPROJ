package com.university.timetable_scheduler.mapper;

import com.university.timetable_scheduler.dto.response.lecturer.LecturerResponse;
import com.university.timetable_scheduler.entity.Lecturer;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LecturerMapper {
    @Mapping(source = "lecturerDepartment.id", target = "lecturerDepartmentId")
    LecturerResponse toResponse(Lecturer lecturer);

    List<LecturerResponse> toResponseList(List<Lecturer> lecturers);
}

