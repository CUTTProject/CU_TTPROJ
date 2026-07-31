package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.lecturer.*;
import com.university.timetable_scheduler.dto.response.lecturer.*;
import com.university.timetable_scheduler.entity.Department;
import com.university.timetable_scheduler.entity.Lecturer;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.mapper.LecturerMapper;
import com.university.timetable_scheduler.repository.DepartmentRepository;
import com.university.timetable_scheduler.repository.LecturerRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.service.LecturerService;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@AllArgsConstructor
public class LecturerServiceImpl implements LecturerService {
    private final LecturerRepository lecturerRepository;
    private final DepartmentRepository departmentRepository;
    private final LecturerMapper lecturerMapper;
    private final SchoolRepository schoolRepository;

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
}
