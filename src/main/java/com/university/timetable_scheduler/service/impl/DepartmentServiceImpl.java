package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.bulk.BulkRow;
import com.university.timetable_scheduler.bulk.BulkUploadOptions;
import com.university.timetable_scheduler.bulk.BulkUploadReport;
import com.university.timetable_scheduler.bulk.BulkUploadSupport;
import com.university.timetable_scheduler.bulk.BulkValues;
import com.university.timetable_scheduler.dto.request.department.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.department.*;
import com.university.timetable_scheduler.entity.Department;
import com.university.timetable_scheduler.entity.Lecturer;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.mapper.DepartmentMapper;
import com.university.timetable_scheduler.repository.DepartmentRepository;
import com.university.timetable_scheduler.repository.LecturerRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.service.DepartmentService;
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
import java.util.UUID;

@Service
@AllArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    private final SchoolRepository schoolRepository;
    private final LecturerRepository lecturerRepository;
    private final BulkUploadSupport bulkUploadSupport;

    private Lecturer findLecturer(UUID lecturerId) {
        return lecturerRepository.findByIdAndSchoolId(lecturerId, TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department head lecturer not found"));
    }

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    private void ensureCodeAvailable(String departmentCode, UUID excludeId) {
        if (departmentRepository.existsLiveByDepartmentCode(TenantContext.getSchoolId(), departmentCode, excludeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Department code '" + departmentCode + "' already exists");
        }
    }

    @Override
    public CreateDepartmentResponse createDepartment(CreateDepartmentRequest request) {
        String departmentCode = normalizeCode(request.getDepartmentCode());
        ensureCodeAvailable(departmentCode, null);
        Department entity = new Department();
        entity.setSchool(currentSchool());
        entity.setDepartmentName(request.getDepartmentName());
        entity.setDepartmentCode(departmentCode);
        if (request.getDepartmentHeadId() != null) {
            entity.setDepartmentHead(findLecturer(request.getDepartmentHeadId()));
        }
        Department saved = departmentRepository.save(entity);
        CreateDepartmentResponse response = new CreateDepartmentResponse();
        CreateDepartmentResponse.Data responseData = new CreateDepartmentResponse.Data();
        responseData.setDepartment(departmentMapper.toResponse(saved));
        response.setData(responseData);
        return response;
    }

    @Override
    public ReadDepartmentResponse readDepartment(ReadDepartmentRequest request) {
        List<Department> list = departmentRepository.findDepartmentByFilter(
                TenantContext.getSchoolId(),
                request.getId(), request.getDepartmentName(), request.getDepartmentCode(),
                request.getDepartmentStatus());
        ReadDepartmentResponse response = new ReadDepartmentResponse();
        ReadDepartmentResponse.Data responseData = new ReadDepartmentResponse.Data();
        responseData.setDepartments(departmentMapper.toResponseList(list));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public UpdateDepartmentResponse updateDepartment(UpdateDepartmentRequest request) {
        Department entity = departmentRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        if (request.getDepartmentCode() != null) {
            if (request.getDepartmentCode().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department code cannot be blank");
            }
            request.setDepartmentCode(normalizeCode(request.getDepartmentCode()));
            ensureCodeAvailable(request.getDepartmentCode(), entity.getId());
        }
        if (request.getDepartmentHeadId() != null) {
            entity.setDepartmentHead(findLecturer(request.getDepartmentHeadId()));
        }
        departmentMapper.updateDtoToEntity(request, entity);
        UpdateDepartmentResponse response = new UpdateDepartmentResponse();
        UpdateDepartmentResponse.Data responseData = new UpdateDepartmentResponse.Data();
        responseData.setDepartment(departmentMapper.toResponse(entity));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public DeleteDepartmentResponse deleteDepartment(DeleteDepartmentRequest request) {
        Department entity = departmentRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        entity.setIsDeleted(true);
        return new DeleteDepartmentResponse();
    }

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadDepartments(MultipartFile file, boolean dryRun) {
        return bulkUploadSupport.importCsv(file, BulkUploadDepartmentArrayRequest.Row.class,
                BulkUploadOptions.of(BulkUploadEnum.BulkDataset.DEPARTMENTS, dryRun), this::processDepartmentRows);
    }

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadDepartmentsArray(BulkUploadDepartmentArrayRequest request, boolean dryRun) {
        return bulkUploadSupport.importRows(request.getRows(),
                BulkUploadOptions.of(BulkUploadEnum.BulkDataset.DEPARTMENTS, dryRun), this::processDepartmentRows);
    }

    /**
     * Upserts on {@code departmentCode}. The head is optional and only a warning when unknown:
     * lecturers are uploaded after departments, so a first upload usually cannot name one.
     */
    private void processDepartmentRows(List<BulkRow<BulkUploadDepartmentArrayRequest.Row>> rows,
                                       BulkUploadReport report) {
        School school = currentSchool();
        Map<String, Department> byCode =
                BulkValues.index(departmentRepository.findAllBySchool_Id(school.getId()), Department::getDepartmentCode);
        Map<String, Lecturer> lecturersByStaffNumber =
                BulkValues.index(lecturerRepository.findAllBySchool_Id(school.getId()), Lecturer::getLecturerStaffNumber);

        Map<String, Integer> firstRowByCode = new HashMap<>();
        List<Department> toSave = new ArrayList<>();

        for (BulkRow<BulkUploadDepartmentArrayRequest.Row> bulkRow : rows) {
            BulkUploadDepartmentArrayRequest.Row row = bulkRow.data();
            String code = normalizeCode(row.getDepartmentCode());

            Integer earlierRow = firstRowByCode.putIfAbsent(code, bulkRow.rowNumber());
            if (earlierRow != null) {
                report.reject(bulkRow, "departmentCode", row.getDepartmentCode(),
                        "Already given on row " + earlierRow + "; each department may appear once");
                continue;
            }

            Department department = byCode.get(code);
            boolean isNew = department == null;
            if (isNew) {
                department = new Department();
                department.setSchool(school);
                department.setDepartmentCode(code);
            }
            department.setDepartmentName(row.getDepartmentName().trim());

            if (!BulkValues.isBlank(row.getDepartmentHeadStaffNumber())) {
                Lecturer head = lecturersByStaffNumber.get(BulkValues.key(row.getDepartmentHeadStaffNumber()));
                if (head == null) {
                    report.warn(bulkRow, "departmentHeadStaffNumber", row.getDepartmentHeadStaffNumber(),
                            "No lecturer has this staff number; head left unchanged. Upload lecturers, then re-upload departments");
                } else {
                    department.setDepartmentHead(head);
                }
            }

            toSave.add(department);
            if (isNew) report.created(); else report.updated();
        }

        departmentRepository.saveAll(toSave);
    }
}
