package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.student.*;
import com.university.timetable_scheduler.dto.response.student.*;
import com.university.timetable_scheduler.entity.Department;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.entity.Student;
import com.university.timetable_scheduler.mapper.StudentMapper;
import com.university.timetable_scheduler.repository.DepartmentRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.repository.StudentRepository;
import com.university.timetable_scheduler.service.StudentService;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@AllArgsConstructor
public class StudentServiceImpl implements StudentService {
    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentMapper studentMapper;
    private final SchoolRepository schoolRepository;

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    @Override
    public CreateStudentResponse createStudent(CreateStudentRequest request) {
        Department department = departmentRepository.findByIdAndSchoolId(request.getStudentDepartmentId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        Student entity = new Student();
        entity.setSchool(currentSchool());
        entity.setStudentFirstName(request.getStudentFirstName());
        entity.setStudentLastName(request.getStudentLastName());
        entity.setStudentMatriculationNumber(request.getStudentMatriculationNumber());
        entity.setStudentEmail(request.getStudentEmail());
        entity.setStudentLevel(request.getStudentLevel());
        entity.setStudentDepartment(department);
        Student saved = studentRepository.save(entity);
        CreateStudentResponse response = new CreateStudentResponse();
        CreateStudentResponse.Data responseData = new CreateStudentResponse.Data();
        responseData.setStudent(studentMapper.toResponse(saved));
        response.setData(responseData);
        return response;
    }

    @Override
    public ReadStudentResponse readStudent(ReadStudentRequest request) {
        List<Student> list = studentRepository.findStudentByFilter(
                TenantContext.getSchoolId(),
                request.getId(), request.getStudentFirstName(), request.getStudentLastName(),
                request.getStudentMatriculationNumber(), request.getStudentEmail(),
                request.getStudentLevel(), request.getStudentDepartmentId(), request.getStudentStatus());
        ReadStudentResponse response = new ReadStudentResponse();
        ReadStudentResponse.Data responseData = new ReadStudentResponse.Data();
        responseData.setStudents(studentMapper.toResponseList(list));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public UpdateStudentResponse updateStudent(UpdateStudentRequest request) {
        Student entity = studentRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        if (request.getStudentFirstName() != null) entity.setStudentFirstName(request.getStudentFirstName());
        if (request.getStudentLastName() != null) entity.setStudentLastName(request.getStudentLastName());
        if (request.getStudentMatriculationNumber() != null) entity.setStudentMatriculationNumber(request.getStudentMatriculationNumber());
        if (request.getStudentEmail() != null) entity.setStudentEmail(request.getStudentEmail());
        if (request.getStudentLevel() != null) entity.setStudentLevel(request.getStudentLevel());
        if (request.getStudentDepartmentId() != null) {
            Department department = departmentRepository.findByIdAndSchoolId(request.getStudentDepartmentId(), TenantContext.getSchoolId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
            entity.setStudentDepartment(department);
        }
        UpdateStudentResponse response = new UpdateStudentResponse();
        UpdateStudentResponse.Data responseData = new UpdateStudentResponse.Data();
        responseData.setStudent(studentMapper.toResponse(entity));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public DeleteStudentResponse deleteStudent(DeleteStudentRequest request) {
        Student entity = studentRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        entity.setIsDeleted(true);
        return new DeleteStudentResponse();
    }
}
