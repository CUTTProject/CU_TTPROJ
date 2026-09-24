package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.bulk.BulkRow;
import com.university.timetable_scheduler.bulk.BulkUploadOptions;
import com.university.timetable_scheduler.bulk.BulkUploadReport;
import com.university.timetable_scheduler.bulk.BulkUploadSupport;
import com.university.timetable_scheduler.bulk.BulkValues;
import com.university.timetable_scheduler.dto.request.programcourse.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.programcourse.*;
import com.university.timetable_scheduler.entity.Course;
import com.university.timetable_scheduler.entity.Program;
import com.university.timetable_scheduler.entity.ProgramCourse;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.mapper.ProgramCourseMapper;
import com.university.timetable_scheduler.repository.CourseRepository;
import com.university.timetable_scheduler.repository.ProgramCourseRepository;
import com.university.timetable_scheduler.repository.ProgramRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.service.ProgramCourseService;
import com.university.timetable_scheduler.status.BulkUploadEnum;
import com.university.timetable_scheduler.status.CourseEnum;
import com.university.timetable_scheduler.status.ProgramCourseEnum;
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

/** A programme's curriculum. Reference data: timetable generation does not read it. */
@Service
@AllArgsConstructor
public class ProgramCourseServiceImpl implements ProgramCourseService {
    private final ProgramCourseRepository programCourseRepository;
    private final ProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final SchoolRepository schoolRepository;
    private final ProgramCourseMapper programCourseMapper;
    private final BulkUploadSupport bulkUploadSupport;

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    @Override
    @Transactional
    public CreateProgramCourseResponse createProgramCourse(CreateProgramCourseRequest request) {
        UUID schoolId = TenantContext.getSchoolId();
        Program program = programRepository.findByIdAndSchoolId(request.getProgramId(), schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));
        Course course = courseRepository.findByIdAndSchoolId(request.getCourseId(), schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        if (programCourseRepository.existsLive(schoolId, program.getId(), course.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This course is already in the programme's curriculum");
        }

        ProgramCourse entity = new ProgramCourse();
        entity.setSchool(currentSchool());
        entity.setProgramCourseProgram(program);
        entity.setProgramCourseCourse(course);
        entity.setProgramCourseLevel(request.getLevel());
        entity.setProgramCourseSemester(request.getSemester());
        entity.setProgramCourseIsCore(request.getIsCore() == null || request.getIsCore());
        ProgramCourse saved = programCourseRepository.save(entity);

        CreateProgramCourseResponse response = new CreateProgramCourseResponse();
        response.setData(new CreateProgramCourseResponse.Data(programCourseMapper.toResponse(saved)));
        return response;
    }

    @Override
    public ReadProgramCourseResponse readProgramCourse(ReadProgramCourseRequest request) {
        List<ProgramCourse> list = programCourseRepository.findProgramCourseByFilter(TenantContext.getSchoolId(),
                request.getProgramId(), request.getCourseId(), request.getLevel(), request.getSemester());
        ReadProgramCourseResponse response = new ReadProgramCourseResponse();
        response.setData(new ReadProgramCourseResponse.Data(programCourseMapper.toResponseList(list)));
        return response;
    }

    @Override
    @Transactional
    public DeleteProgramCourseResponse deleteProgramCourse(DeleteProgramCourseRequest request) {
        ProgramCourse entity = programCourseRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curriculum entry not found"));
        entity.setIsDeleted(true);
        return new DeleteProgramCourseResponse();
    }

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadProgramCourses(MultipartFile file, boolean dryRun) {
        return bulkUploadSupport.importCsv(file, BulkUploadProgramCourseArrayRequest.Row.class,
                BulkUploadOptions.of(BulkUploadEnum.BulkDataset.CURRICULUM, dryRun), this::processProgramCourseRows);
    }

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadProgramCoursesArray(BulkUploadProgramCourseArrayRequest request, boolean dryRun) {
        return bulkUploadSupport.importRows(request.getRows(),
                BulkUploadOptions.of(BulkUploadEnum.BulkDataset.CURRICULUM, dryRun), this::processProgramCourseRows);
    }

    /** Upserts on programme and course. A blank {@code isCore} means core on create, unchanged on update. */
    private void processProgramCourseRows(List<BulkRow<BulkUploadProgramCourseArrayRequest.Row>> rows,
                                          BulkUploadReport report) {
        School school = currentSchool();
        UUID schoolId = school.getId();
        Map<String, Program> programsByCode =
                BulkValues.index(programRepository.findAllBySchool_Id(schoolId), Program::getProgramCode);
        Map<String, Course> coursesByCode =
                BulkValues.index(courseRepository.findAllBySchool_Id(schoolId), Course::getCourseCode);
        Map<String, ProgramCourse> existing = BulkValues.index(programCourseRepository.findAllBySchool_Id(schoolId),
                pc -> pc.getProgramCourseProgram().getId() + "|" + pc.getProgramCourseCourse().getId());

        Map<String, Integer> firstRowByKey = new HashMap<>();
        List<ProgramCourse> toSave = new ArrayList<>();

        for (BulkRow<BulkUploadProgramCourseArrayRequest.Row> bulkRow : rows) {
            BulkUploadProgramCourseArrayRequest.Row row = bulkRow.data();

            Program program = programsByCode.get(BulkValues.key(row.getProgramCode()));
            if (program == null) {
                report.reject(bulkRow, "programCode", row.getProgramCode(), "No programme has this code");
            }
            Course course = coursesByCode.get(BulkValues.key(row.getCourseCode()));
            if (course == null) {
                report.reject(bulkRow, "courseCode", row.getCourseCode(), "No course has this code");
            }
            CourseEnum.CourseLevel level = BulkValues.parseEnum(CourseEnum.CourseLevel.class,
                    row.getLevel(), bulkRow, "level", report);
            ProgramCourseEnum.Semester semester = BulkValues.parseEnum(ProgramCourseEnum.Semester.class,
                    row.getSemester(), bulkRow, "semester", report);
            Boolean isCore = BulkValues.parseBoolean(row.getIsCore(), bulkRow, "isCore", report);
            if (report.isRejected(bulkRow)) {
                continue;
            }

            String key = BulkValues.key(program.getId() + "|" + course.getId());
            Integer earlierRow = firstRowByKey.putIfAbsent(key, bulkRow.rowNumber());
            if (earlierRow != null) {
                report.reject(bulkRow, "courseCode", row.getCourseCode(),
                        "Already given for this programme on row " + earlierRow);
                continue;
            }

            ProgramCourse entry = existing.get(key);
            boolean isNew = entry == null;
            if (isNew) {
                entry = new ProgramCourse();
                entry.setSchool(school);
                entry.setProgramCourseProgram(program);
                entry.setProgramCourseCourse(course);
                entry.setProgramCourseIsCore(isCore == null || isCore);
            } else if (isCore != null) {
                entry.setProgramCourseIsCore(isCore);
            }
            entry.setProgramCourseLevel(level);
            if (semester != null) entry.setProgramCourseSemester(semester);

            toSave.add(entry);
            if (isNew) report.created(); else report.updated();
        }

        programCourseRepository.saveAll(toSave);
    }
}
