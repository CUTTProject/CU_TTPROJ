package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.course.CreateCourseRequest;
import com.university.timetable_scheduler.dto.request.course.DeleteCourseRequest;
import com.university.timetable_scheduler.dto.request.course.ReadCourseRequest;
import com.university.timetable_scheduler.dto.request.course.UpdateCourseRequest;
import com.university.timetable_scheduler.dto.response.course.CreateCourseResponse;
import com.university.timetable_scheduler.dto.response.course.DeleteCourseResponse;
import com.university.timetable_scheduler.dto.response.course.ReadCourseResponse;
import com.university.timetable_scheduler.dto.response.course.UpdateCourseResponse;

public interface CourseService {
    CreateCourseResponse createCourse (CreateCourseRequest createCourseRequest);
    ReadCourseResponse readCourse (ReadCourseRequest readCourseRequest);
    UpdateCourseResponse updateCourse (UpdateCourseRequest updateCourseRequest);
    DeleteCourseResponse deleteCourse (DeleteCourseRequest deleteCourseRequest);
}
