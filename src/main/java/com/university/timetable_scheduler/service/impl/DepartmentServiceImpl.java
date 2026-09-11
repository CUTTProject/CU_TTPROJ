package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.department.*;
import com.university.timetable_scheduler.dto.response.department.*;
import com.university.timetable_scheduler.entity.Department;
import com.university.timetable_scheduler.entity.Lecturer;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.mapper.DepartmentMapper;
import com.university.timetable_scheduler.repository.DepartmentRepository;
import com.university.timetable_scheduler.repository.LecturerRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.service.DepartmentService;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    private final SchoolRepository schoolRepository;
    private final LecturerRepository lecturerRepository;

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
}