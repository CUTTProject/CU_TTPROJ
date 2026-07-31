package com.university.timetable_scheduler.service.impl;

import com.opencsv.bean.CsvToBeanBuilder;
import com.university.timetable_scheduler.dto.request.enrollment.*;
import com.university.timetable_scheduler.dto.response.enrollment.*;
import com.university.timetable_scheduler.entity.Department;
import com.university.timetable_scheduler.entity.Enrollment;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.entity.Section;
import com.university.timetable_scheduler.entity.Student;
import com.university.timetable_scheduler.mapper.EnrollmentMapper;
import com.university.timetable_scheduler.repository.*;
import com.university.timetable_scheduler.service.EnrollmentService;
import com.university.timetable_scheduler.status.StudentEnum;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SectionRepository sectionRepository;
    private final DepartmentRepository departmentRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final SchoolRepository schoolRepository;

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    @Override
    public CreateEnrollmentResponse createEnrollment(CreateEnrollmentRequest request) {
        UUID schoolId = TenantContext.getSchoolId();
        Student student = studentRepository.findByIdAndSchoolId(request.getEnrollmentStudentId(), schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        Section section = sectionRepository.findByIdAndSchoolId(request.getEnrollmentSectionId(), schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        Enrollment entity = new Enrollment();
        entity.setSchool(currentSchool());
        entity.setEnrollmentStudent(student);
        entity.setEnrollmentSection(section);
        Enrollment saved = enrollmentRepository.save(entity);
        CreateEnrollmentResponse response = new CreateEnrollmentResponse();
        CreateEnrollmentResponse.Data responseData = new CreateEnrollmentResponse.Data();
        responseData.setEnrollment(enrollmentMapper.toResponse(saved));
        response.setData(responseData);
        return response;
    }

    @Override
    public ReadEnrollmentResponse readEnrollment(ReadEnrollmentRequest request) {
        List<Enrollment> list = enrollmentRepository.findEnrollmentByFilter(
                TenantContext.getSchoolId(),
                request.getId(), request.getEnrollmentStudentId(), request.getEnrollmentSectionId(), request.getEnrollmentStatus());
        ReadEnrollmentResponse response = new ReadEnrollmentResponse();
        ReadEnrollmentResponse.Data responseData = new ReadEnrollmentResponse.Data();
        responseData.setEnrollments(enrollmentMapper.toResponseList(list));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public UpdateEnrollmentResponse updateEnrollment(UpdateEnrollmentRequest request) {
        UUID schoolId = TenantContext.getSchoolId();
        Enrollment entity = enrollmentRepository.findByIdAndSchoolId(request.getId(), schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));
        if (request.getEnrollmentStudentId() != null) {
            Student student = studentRepository.findByIdAndSchoolId(request.getEnrollmentStudentId(), schoolId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
            entity.setEnrollmentStudent(student);
        }
        if (request.getEnrollmentSectionId() != null) {
            Section section = sectionRepository.findByIdAndSchoolId(request.getEnrollmentSectionId(), schoolId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
            entity.setEnrollmentSection(section);
        }
        UpdateEnrollmentResponse response = new UpdateEnrollmentResponse();
        UpdateEnrollmentResponse.Data responseData = new UpdateEnrollmentResponse.Data();
        responseData.setEnrollment(enrollmentMapper.toResponse(entity));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public DeleteEnrollmentResponse deleteEnrollment(DeleteEnrollmentRequest request) {
        Enrollment entity = enrollmentRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));
        entity.setIsDeleted(true);
        return new DeleteEnrollmentResponse();
    }

    @Override
    @Transactional
    public BulkEnrollmentResponse bulkEnroll(BulkEnrollmentRequest request) {
        School school = currentSchool();
        UUID schoolId = school.getId();
        List<EnrollmentResponse> created = new ArrayList<>();
        List<String> skippedReasons = new ArrayList<>();

        for (BulkEnrollmentRequest.Row row : request.getRows()) {
            // Resolve department scoped to school
            List<Department> departments = departmentRepository.findDepartmentByFilter(schoolId, null, row.getStudentDepartment(), null);
            if (departments.isEmpty()) {
                skippedReasons.add("Row [" + row.getStudentMatriculationNumber() + " → " + row.getCourseSection() + "]: Department '" + row.getStudentDepartment() + "' not found");
                continue;
            }
            Department department = departments.get(0);

            // Resolve section by name AND academic period, scoped to school
            List<Section> sections = sectionRepository.findSectionByFilter(schoolId, null, null, row.getCourseSection(), request.getAcademicPeriodId(), null);
            if (sections.isEmpty()) {
                skippedReasons.add("Row [" + row.getStudentMatriculationNumber() + " → " + row.getCourseSection() + "]: Section '" + row.getCourseSection() + "' not found in the specified academic period");
                continue;
            }
            Section section = sections.get(0);

            // Find or create student
            Student student = studentRepository
                    .findByMatriculationNumberForTenant(row.getStudentMatriculationNumber(), schoolId)
                    .orElseGet(() -> {
                        Student s = new Student();
                        s.setSchool(school);
                        s.setStudentMatriculationNumber(row.getStudentMatriculationNumber());
                        s.setStudentFirstName(row.getStudentFirstName());
                        s.setStudentLastName(row.getStudentLastName());
                        s.setStudentEmail(row.getStudentEmail());
                        s.setStudentLevel(row.getStudentLevel());
                        s.setStudentDepartment(department);
                        return studentRepository.save(s);
                    });

            // Skip if already enrolled
            if (enrollmentRepository.existsForTenant(schoolId, student.getId(), section.getId())) {
                skippedReasons.add("Row [" + row.getStudentMatriculationNumber() + " → " + row.getCourseSection() + "]: Already enrolled");
                continue;
            }

            Enrollment enrollment = new Enrollment();
            enrollment.setSchool(school);
            enrollment.setEnrollmentStudent(student);
            enrollment.setEnrollmentSection(section);
            Enrollment saved = enrollmentRepository.save(enrollment);
            created.add(enrollmentMapper.toResponse(saved));
        }

        BulkEnrollmentResponse response = new BulkEnrollmentResponse();
        BulkEnrollmentResponse.Data data = new BulkEnrollmentResponse.Data();
        data.setTotalProcessed(request.getRows().size());
        data.setTotalCreated(created.size());
        data.setTotalSkipped(skippedReasons.size());
        data.setEnrollments(created);
        data.setSkippedReasons(skippedReasons);
        response.setData(data);
        return response;
    }

    @Override
    @Transactional
    public BulkEnrollmentResponse bulkEnrollFromFile(MultipartFile file, UUID academicPeriodId) {
        List<BulkEnrollmentFileRequest> csvRows;
        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            csvRows = new CsvToBeanBuilder<BulkEnrollmentFileRequest>(reader)
                    .withType(BulkEnrollmentFileRequest.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse enrollment CSV: " + e.getMessage(), e);
        }

        List<BulkEnrollmentRequest.Row> rows = new ArrayList<>();
        for (BulkEnrollmentFileRequest csvRow : csvRows) {
            BulkEnrollmentRequest.Row row = new BulkEnrollmentRequest.Row();
            row.setStudentMatriculationNumber(csvRow.getStudentMatriculationNumber());
            row.setStudentFirstName(csvRow.getStudentFirstName());
            row.setStudentLastName(csvRow.getStudentLastName());
            row.setStudentEmail(csvRow.getStudentEmail());
            row.setStudentLevel(StudentEnum.StudentLevel.valueOf(
                    csvRow.getStudentLevel().trim().toUpperCase()));
            row.setStudentDepartment(csvRow.getStudentDepartment());
            row.setCourseSection(csvRow.getCourseSection());
            rows.add(row);
        }

        BulkEnrollmentRequest bulkRequest = new BulkEnrollmentRequest();
        bulkRequest.setAcademicPeriodId(academicPeriodId);
        bulkRequest.setRows(rows);
        return bulkEnroll(bulkRequest);
    }
}
