package com.university.timetable_scheduler.mapper;

import com.university.timetable_scheduler.dto.request.course.UpdateCourseRequest;
import com.university.timetable_scheduler.dto.response.course.CourseResponse;
import com.university.timetable_scheduler.entity.Course;
import org.mapstruct.*;

import java.util.List;

@Mapper(
    componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface CourseMapper {
    CourseResponse toResponse(Course course);
    List<CourseResponse> toResponseList(List<Course> courses);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE
    )
    void updateCourseDtoToCourse (UpdateCourseRequest updateCourseRequest, @MappingTarget Course course);
}
