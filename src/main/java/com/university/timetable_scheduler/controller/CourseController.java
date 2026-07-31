package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.request.course.CreateCourseRequest;
import com.university.timetable_scheduler.dto.request.course.DeleteCourseRequest;
import com.university.timetable_scheduler.dto.request.course.ReadCourseRequest;
import com.university.timetable_scheduler.dto.request.course.UpdateCourseRequest;
import com.university.timetable_scheduler.dto.response.course.CreateCourseResponse;
import com.university.timetable_scheduler.dto.response.course.DeleteCourseResponse;
import com.university.timetable_scheduler.dto.response.course.ReadCourseResponse;
import com.university.timetable_scheduler.dto.response.course.UpdateCourseResponse;
import com.university.timetable_scheduler.service.impl.CourseServiceImpl;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/courses")
@AllArgsConstructor
public class CourseController {
    private final CourseServiceImpl courseService;

    @PostMapping("/create")
    public CreateCourseResponse createCourse (@Valid @RequestBody CreateCourseRequest createCourseRequest){
        return courseService.createCourse(createCourseRequest);
    }

    @GetMapping("/read")
    public ReadCourseResponse readCourse (@Valid @ModelAttribute ReadCourseRequest readCourseRequest){
        return courseService.readCourse(readCourseRequest);
    }

    @PutMapping("/update")
    public UpdateCourseResponse updateCourse (@Valid @RequestBody UpdateCourseRequest updateCourseRequest){
        return courseService.updateCourse(updateCourseRequest);
    }

    @DeleteMapping("/delete")
    public DeleteCourseResponse deleteCourse (@Valid @ModelAttribute DeleteCourseRequest deleteCourseResponse){
        return courseService.deleteCourse(deleteCourseResponse);
    }
}