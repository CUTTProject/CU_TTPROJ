package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.course.*;
import com.university.timetable_scheduler.dto.response.course.*;
import com.university.timetable_scheduler.entity.Course;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.mapper.CourseMapper;
import com.university.timetable_scheduler.repository.CourseRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.service.CourseService;
import com.university.timetable_scheduler.status.CourseEnum;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@AllArgsConstructor
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final SchoolRepository schoolRepository;

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    @Override
    public CreateCourseResponse createCourse(CreateCourseRequest createCourseRequest) {
        Course newCourse = new Course();
        newCourse.setSchool(currentSchool());
        newCourse.setCourseName(createCourseRequest.getCourseName());
        newCourse.setCourseCode(createCourseRequest.getCourseCode());
        newCourse.setCourseLevel(createCourseRequest.getCourseLevel());
        newCourse.setCourseDescription(createCourseRequest.getCourseDescription());
        newCourse.setCourseUnit(createCourseRequest.getCourseUnit());
        newCourse.setCourseStatus(CourseEnum.CourseStatus.ACTIVE);
        Course course = courseRepository.save(newCourse);
        CreateCourseResponse response = new CreateCourseResponse();
        CreateCourseResponse.Data responseData = new CreateCourseResponse.Data();
        responseData.setCourse(courseMapper.toResponse(course));
        response.setData(responseData);
        return response;
    }

    @Override
    public ReadCourseResponse readCourse(ReadCourseRequest readCourseRequest) {
        List<Course> courses = courseRepository.findCourseByFilter(
                TenantContext.getSchoolId(),
                readCourseRequest.getCourseId(), readCourseRequest.getCourseCode(),
                readCourseRequest.getCourseName(), readCourseRequest.getCourseUnit(),
                readCourseRequest.getCourseLevel(), readCourseRequest.getCourseStatus());
        ReadCourseResponse response = new ReadCourseResponse();
        ReadCourseResponse.Data responseData = new ReadCourseResponse.Data();
        responseData.setCourses(courseMapper.toResponseList(courses));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public UpdateCourseResponse updateCourse(UpdateCourseRequest updateCourseRequest) {
        Course course = courseRepository.findByIdAndSchoolId(updateCourseRequest.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        courseMapper.updateCourseDtoToCourse(updateCourseRequest, course);
        UpdateCourseResponse response = new UpdateCourseResponse();
        UpdateCourseResponse.Data responseData = new UpdateCourseResponse.Data();
        responseData.setCourse(courseMapper.toResponse(course));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public DeleteCourseResponse deleteCourse(DeleteCourseRequest deleteCourseRequest) {
        Course course = courseRepository.findByIdAndSchoolId(deleteCourseRequest.getCourseId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        course.setIsDeleted(true);
        return new DeleteCourseResponse();
    }
}