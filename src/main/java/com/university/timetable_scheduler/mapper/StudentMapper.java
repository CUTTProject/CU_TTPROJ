package com.university.timetable_scheduler.mapper;

import com.university.timetable_scheduler.dto.response.student.StudentResponse;
import com.university.timetable_scheduler.entity.Student;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StudentMapper {
    @Mapping(source = "studentDepartment.id", target = "studentDepartmentId")
    StudentResponse toResponse(Student student);

    List<StudentResponse> toResponseList(List<Student> students);
}

