package com.university.timetable_scheduler.mapper;

import com.university.timetable_scheduler.dto.response.programcourse.ProgramCourseResponse;
import com.university.timetable_scheduler.entity.ProgramCourse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProgramCourseMapper {
    @Mapping(source = "programCourseProgram.id", target = "programId")
    @Mapping(source = "programCourseProgram.programCode", target = "programCode")
    @Mapping(source = "programCourseProgram.programName", target = "programName")
    @Mapping(source = "programCourseCourse.id", target = "courseId")
    @Mapping(source = "programCourseCourse.courseCode", target = "courseCode")
    @Mapping(source = "programCourseCourse.courseName", target = "courseName")
    @Mapping(source = "programCourseLevel", target = "level")
    @Mapping(source = "programCourseSemester", target = "semester")
    @Mapping(source = "programCourseIsCore", target = "isCore")
    ProgramCourseResponse toResponse(ProgramCourse programCourse);

    List<ProgramCourseResponse> toResponseList(List<ProgramCourse> programCourses);
}
