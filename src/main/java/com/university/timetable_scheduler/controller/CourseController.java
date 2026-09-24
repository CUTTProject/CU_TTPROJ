package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.request.course.BulkUploadCourseArrayRequest;
import com.university.timetable_scheduler.dto.request.course.CreateCourseRequest;
import com.university.timetable_scheduler.dto.request.course.DeleteCourseRequest;
import com.university.timetable_scheduler.dto.request.course.ReadCourseRequest;
import com.university.timetable_scheduler.dto.request.course.UpdateCourseRequest;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.course.CreateCourseResponse;
import com.university.timetable_scheduler.dto.response.course.DeleteCourseResponse;
import com.university.timetable_scheduler.dto.response.course.ReadCourseResponse;
import com.university.timetable_scheduler.dto.response.course.UpdateCourseResponse;
import com.university.timetable_scheduler.service.impl.CourseServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


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

    @Operation(summary = "Bulk upload courses from a CSV file. Columns: courseCode, courseName, courseUnit, "
            + "courseLevel (LEVEL_100 ... or 100 ...), departmentCode, courseDescription. Upserts by "
            + "courseCode; courseUnit and courseLevel are required only for new courses. Valid rows are"
            + " saved; rejected rows are listed with the reason. Pass dryRun=true to validate and see "
            + "the counts without saving.")
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BulkUploadResponse bulkUploadCourses(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return courseService.bulkUploadCourses(file, dryRun);
    }

    @Operation(summary = "Bulk upload courses from a JSON array. Rows use the CSV column names. Pass dryRun=true "
            + "to validate and see the counts without saving.")
    @PostMapping(value = "/bulk-upload/array", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BulkUploadResponse bulkUploadCoursesArray(
            @Valid @RequestBody BulkUploadCourseArrayRequest request,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return courseService.bulkUploadCoursesArray(request, dryRun);
    }
}
