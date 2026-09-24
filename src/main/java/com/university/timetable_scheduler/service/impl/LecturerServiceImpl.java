package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.bulk.BulkRow;
import com.university.timetable_scheduler.bulk.BulkUploadOptions;
import com.university.timetable_scheduler.bulk.BulkUploadReport;
import com.university.timetable_scheduler.bulk.BulkUploadSupport;
import com.university.timetable_scheduler.bulk.BulkValues;
import com.university.timetable_scheduler.dto.request.lecturer.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.lecturer.*;
import com.university.timetable_scheduler.entity.Department;
import com.university.timetable_scheduler.entity.Lecturer;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.mapper.LecturerMapper;
import com.university.timetable_scheduler.repository.DepartmentRepository;
import com.university.timetable_scheduler.repository.LecturerRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.service.LecturerService;
import com.university.timetable_scheduler.status.ActivityEnum;
import com.university.timetable_scheduler.status.BulkUploadEnum;
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

@Service
@AllArgsConstructor
public class LecturerServiceImpl implements LecturerService {
    private final LecturerRepository lecturerRepository;
    private final DepartmentRepository departmentRepository;
    private final LecturerMapper lecturerMapper;
    private final SchoolRepository schoolRepository;
    private final ActivityServiceImpl activityService;
    private final BulkUploadSupport bulkUploadSupport;

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    @Override
    public CreateLecturerResponse createLecturer(CreateLecturerRequest request) {
        Department department = departmentRepository.findByIdAndSchoolId(request.getLecturerDepartmentId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        Lecturer entity = new Lecturer();
        entity.setSchool(currentSchool());
        entity.setLecturerStaffNumber(request.getLecturerStaffNumber());
        entity.setLecturerFirstName(request.getLecturerFirstName());
        entity.setLecturerLastName(request.getLecturerLastName());
        entity.setLecturerEmail(request.getLecturerEmail());
        entity.setLecturerDepartment(department);
        Lecturer saved = lecturerRepository.save(entity);
        activityService.record(ActivityEnum.ActivityType.LECTURER_CREATED, "New lecturer added",
                ActivityServiceImpl.label(saved.getLecturerFirstName(), saved.getLecturerLastName()) + " was added");
        CreateLecturerResponse response = new CreateLecturerResponse();
        CreateLecturerResponse.Data responseData = new CreateLecturerResponse.Data();
        responseData.setLecturer(lecturerMapper.toResponse(saved));
        response.setData(responseData);
        return response;
    }

    @Override
    public ReadLecturerResponse readLecturer(ReadLecturerRequest request) {
        List<Lecturer> list = lecturerRepository.findLecturerByFilter(
                TenantContext.getSchoolId(),
                request.getId(), request.getLecturerStaffNumber(), request.getLecturerFirstName(),
                request.getLecturerLastName(), request.getLecturerEmail(),
                request.getLecturerDepartmentId(), request.getLecturerStatus());
        ReadLecturerResponse response = new ReadLecturerResponse();
        ReadLecturerResponse.Data responseData = new ReadLecturerResponse.Data();
        responseData.setLecturers(lecturerMapper.toResponseList(list));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public UpdateLecturerResponse updateLecturer(UpdateLecturerRequest request) {
        Lecturer entity = lecturerRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lecturer not found"));
        if (request.getLecturerStaffNumber() != null) entity.setLecturerStaffNumber(request.getLecturerStaffNumber());
        if (request.getLecturerFirstName() != null) entity.setLecturerFirstName(request.getLecturerFirstName());
        if (request.getLecturerLastName() != null) entity.setLecturerLastName(request.getLecturerLastName());
        if (request.getLecturerEmail() != null) entity.setLecturerEmail(request.getLecturerEmail());
        if (request.getLecturerDepartmentId() != null) {
            Department department = departmentRepository.findByIdAndSchoolId(request.getLecturerDepartmentId(), TenantContext.getSchoolId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
            entity.setLecturerDepartment(department);
        }
        UpdateLecturerResponse response = new UpdateLecturerResponse();
        UpdateLecturerResponse.Data responseData = new UpdateLecturerResponse.Data();
        responseData.setLecturer(lecturerMapper.toResponse(entity));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public DeleteLecturerResponse deleteLecturer(DeleteLecturerRequest request) {
        Lecturer entity = lecturerRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lecturer not found"));
        entity.setIsDeleted(true);
        return new DeleteLecturerResponse();
    }

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadLecturers(MultipartFile file, boolean dryRun) {
        return bulkUploadSupport.importCsv(file, BulkUploadLecturerArrayRequest.Row.class,
                BulkUploadOptions.of(BulkUploadEnum.BulkDataset.LECTURERS, dryRun), this::processLecturerRows);
    }

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadLecturersArray(BulkUploadLecturerArrayRequest request, boolean dryRun) {
        return bulkUploadSupport.importRows(request.getRows(),
                BulkUploadOptions.of(BulkUploadEnum.BulkDataset.LECTURERS, dryRun), this::processLecturerRows);
    }

    /** Upserts on {@code lecturerStaffNumber}. */
    private void processLecturerRows(List<BulkRow<BulkUploadLecturerArrayRequest.Row>> rows,
                                     BulkUploadReport report) {
        School school = currentSchool();
        Map<String, Lecturer> byStaffNumber =
                BulkValues.index(lecturerRepository.findAllBySchool_Id(school.getId()), Lecturer::getLecturerStaffNumber);
        Map<String, Department> departmentsByCode =
                BulkValues.index(departmentRepository.findAllBySchool_Id(school.getId()), Department::getDepartmentCode);

        Map<String, Integer> firstRowByStaffNumber = new HashMap<>();
        List<Lecturer> toSave = new ArrayList<>();

        for (BulkRow<BulkUploadLecturerArrayRequest.Row> bulkRow : rows) {
            BulkUploadLecturerArrayRequest.Row row = bulkRow.data();
            String staffNumber = BulkValues.key(row.getLecturerStaffNumber());

            Integer earlierRow = firstRowByStaffNumber.putIfAbsent(staffNumber, bulkRow.rowNumber());
            if (earlierRow != null) {
                report.reject(bulkRow, "lecturerStaffNumber", row.getLecturerStaffNumber(),
                        "Already given on row " + earlierRow + "; each lecturer may appear once");
                continue;
            }
            Department department = departmentsByCode.get(BulkValues.key(row.getDepartmentCode()));
            if (department == null) {
                report.reject(bulkRow, "departmentCode", row.getDepartmentCode(), "No department has this code");
                continue;
            }

            Lecturer lecturer = byStaffNumber.get(staffNumber);
            boolean isNew = lecturer == null;
            if (isNew) {
                lecturer = new Lecturer();
                lecturer.setSchool(school);
                lecturer.setLecturerStaffNumber(row.getLecturerStaffNumber().trim());
            }
            lecturer.setLecturerFirstName(row.getLecturerFirstName().trim());
            lecturer.setLecturerLastName(row.getLecturerLastName().trim());
            lecturer.setLecturerEmail(row.getLecturerEmail().trim());
            lecturer.setLecturerDepartment(department);

            toSave.add(lecturer);
            if (isNew) report.created(); else report.updated();
        }

        lecturerRepository.saveAll(toSave);
    }
}
