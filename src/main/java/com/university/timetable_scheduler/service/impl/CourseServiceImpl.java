package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.bulk.BulkRow;
import com.university.timetable_scheduler.bulk.BulkUploadOptions;
import com.university.timetable_scheduler.bulk.BulkUploadReport;
import com.university.timetable_scheduler.bulk.BulkUploadSupport;
import com.university.timetable_scheduler.bulk.BulkValues;
import com.university.timetable_scheduler.dto.request.course.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.course.*;
import com.university.timetable_scheduler.entity.Course;
import com.university.timetable_scheduler.entity.Department;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.mapper.CourseMapper;
import com.university.timetable_scheduler.repository.CourseRepository;
import com.university.timetable_scheduler.repository.DepartmentRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.service.CourseService;
import com.university.timetable_scheduler.status.ActivityEnum;
import com.university.timetable_scheduler.status.BulkUploadEnum;
import com.university.timetable_scheduler.status.CourseEnum;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final SchoolRepository schoolRepository;
    private final ActivityServiceImpl activityService;
    private final DepartmentRepository departmentRepository;
    private final BulkUploadSupport bulkUploadSupport;

    private Department findDepartment(UUID departmentId) {
        return departmentRepository.findByIdAndSchoolId(departmentId, TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
    }

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
        if (createCourseRequest.getCourseDepartmentId() != null) {
            newCourse.setCourseDepartment(findDepartment(createCourseRequest.getCourseDepartmentId()));
        }
        Course course = courseRepository.save(newCourse);
        activityService.record(ActivityEnum.ActivityType.COURSE_CREATED, "New course added",
                ActivityServiceImpl.label(course.getCourseCode(), course.getCourseName()) + " was added");
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
        if (updateCourseRequest.getCourseDepartmentId() != null) {
            course.setCourseDepartment(findDepartment(updateCourseRequest.getCourseDepartmentId()));
        }
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

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadCourses(MultipartFile file, boolean dryRun) {
        return bulkUploadSupport.importCsv(file, BulkUploadCourseArrayRequest.Row.class,
                BulkUploadOptions.of(BulkUploadEnum.BulkDataset.COURSES, dryRun), this::processCourseRows);
    }

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadCoursesArray(BulkUploadCourseArrayRequest request, boolean dryRun) {
        return bulkUploadSupport.importRows(request.getRows(),
                BulkUploadOptions.of(BulkUploadEnum.BulkDataset.COURSES, dryRun), this::processCourseRows);
    }

    /** Upserts on {@code courseCode}. Unit and level are required only when creating. */
    private void processCourseRows(List<BulkRow<BulkUploadCourseArrayRequest.Row>> rows, BulkUploadReport report) {
        School school = currentSchool();
        Map<String, Course> byCode =
                BulkValues.index(courseRepository.findAllBySchool_Id(school.getId()), Course::getCourseCode);
        Map<String, Department> departmentsByCode =
                BulkValues.index(departmentRepository.findAllBySchool_Id(school.getId()), Department::getDepartmentCode);

        Map<String, Integer> firstRowByCode = new HashMap<>();
        List<Course> toSave = new ArrayList<>();

        for (BulkRow<BulkUploadCourseArrayRequest.Row> bulkRow : rows) {
            BulkUploadCourseArrayRequest.Row row = bulkRow.data();
            String code = BulkValues.key(row.getCourseCode());

            Integer earlierRow = firstRowByCode.putIfAbsent(code, bulkRow.rowNumber());
            if (earlierRow != null) {
                report.reject(bulkRow, "courseCode", row.getCourseCode(),
                        "Already given on row " + earlierRow + "; each course may appear once");
                continue;
            }
            Department department = departmentsByCode.get(BulkValues.key(row.getDepartmentCode()));
            if (department == null) {
                report.reject(bulkRow, "departmentCode", row.getDepartmentCode(), "No department has this code");
            }
            CourseEnum.CourseLevel level = BulkValues.parseEnum(CourseEnum.CourseLevel.class,
                    row.getCourseLevel(), bulkRow, "courseLevel", report);

            Course course = byCode.get(code);
            boolean isNew = course == null;
            if (isNew && row.getCourseUnit() == null) {
                report.reject(bulkRow, "courseUnit", null, "courseUnit is required for a new course");
            }
            if (isNew && BulkValues.isBlank(row.getCourseLevel())) {
                report.reject(bulkRow, "courseLevel", null, "courseLevel is required for a new course");
            }
            if (report.isRejected(bulkRow)) {
                continue;
            }

            if (isNew) {
                course = new Course();
                course.setSchool(school);
                course.setCourseCode(row.getCourseCode().trim());
            }
            course.setCourseName(row.getCourseName().trim());
            course.setCourseDepartment(department);
            if (row.getCourseUnit() != null) course.setCourseUnit(row.getCourseUnit());
            if (level != null) course.setCourseLevel(level);
            if (BulkValues.text(row.getCourseDescription()) != null) {
                course.setCourseDescription(BulkValues.text(row.getCourseDescription()));
            }

            toSave.add(course);
            if (isNew) report.created(); else report.updated();
        }

        courseRepository.saveAll(toSave);
    }
}
