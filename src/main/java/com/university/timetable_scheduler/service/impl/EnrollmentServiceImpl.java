package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.bulk.BulkRow;
import com.university.timetable_scheduler.bulk.BulkUploadOptions;
import com.university.timetable_scheduler.bulk.BulkUploadReport;
import com.university.timetable_scheduler.bulk.BulkUploadSupport;
import com.university.timetable_scheduler.bulk.BulkValues;
import com.university.timetable_scheduler.dto.request.enrollment.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.enrollment.*;
import com.university.timetable_scheduler.entity.AcademicPeriod;
import com.university.timetable_scheduler.entity.Enrollment;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.entity.Section;
import com.university.timetable_scheduler.entity.Student;
import com.university.timetable_scheduler.mapper.EnrollmentMapper;
import com.university.timetable_scheduler.repository.*;
import com.university.timetable_scheduler.service.EnrollmentService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@AllArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SectionRepository sectionRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final SchoolRepository schoolRepository;
    private final BulkUploadSupport bulkUploadSupport;

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
    public BulkUploadResponse bulkUploadEnrollments(MultipartFile file, UUID academicPeriodId, boolean dryRun) {
        AcademicPeriod period = findPeriod(academicPeriodId);
        return bulkUploadSupport.importCsv(file, BulkUploadEnrollmentArrayRequest.Row.class,
                new BulkUploadOptions(BulkUploadEnum.BulkDataset.ENROLLMENTS, period.getId(), dryRun),
                (rows, report) -> processEnrollmentRows(rows, report, period));
    }

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadEnrollmentsArray(BulkUploadEnrollmentArrayRequest request, boolean dryRun) {
        AcademicPeriod period = findPeriod(request.getAcademicPeriodId());
        return bulkUploadSupport.importRows(request.getRows(),
                new BulkUploadOptions(BulkUploadEnum.BulkDataset.ENROLLMENTS, period.getId(), dryRun),
                (rows, report) -> processEnrollmentRows(rows, report, period));
    }

    private AcademicPeriod findPeriod(UUID academicPeriodId) {
        return academicPeriodRepository.findByIdAndSchoolId(academicPeriodId, TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Academic period not found: " + academicPeriodId));
    }

    /**
     * Sections are matched on course code and section name within the period, since section names
     * such as "A" repeat across courses. Students and sections must already exist; an enrollment
     * that already exists is counted as unchanged.
     */
    private void processEnrollmentRows(List<BulkRow<BulkUploadEnrollmentArrayRequest.Row>> rows,
                                       BulkUploadReport report, AcademicPeriod period) {
        School school = currentSchool();
        UUID schoolId = school.getId();

        Map<String, Student> studentsByMatric = BulkValues.index(studentRepository.findAllBySchool_Id(schoolId),
                Student::getStudentMatriculationNumber);
        Map<String, Section> sectionsByKey = BulkValues.index(
                sectionRepository.findSectionByFilter(schoolId, null, null, null, period.getId(), null),
                s -> s.getSectionCourse() == null || s.getSectionName() == null
                        ? null : sectionKey(s.getSectionCourse().getCourseCode(), s.getSectionName()));
        Set<String> enrolled = new HashSet<>();
        enrollmentRepository.findAllLiveByAcademicPeriod(schoolId, period.getId()).forEach(e ->
                enrolled.add(e.getEnrollmentStudent().getId() + "|" + e.getEnrollmentSection().getId()));

        Map<String, Integer> firstRowByKey = new HashMap<>();
        List<Enrollment> toSave = new ArrayList<>();

        for (BulkRow<BulkUploadEnrollmentArrayRequest.Row> bulkRow : rows) {
            BulkUploadEnrollmentArrayRequest.Row row = bulkRow.data();
            String sectionKey = BulkValues.key(sectionKey(row.getCourseCode(), row.getSectionName()));

            Integer earlierRow = firstRowByKey.putIfAbsent(
                    BulkValues.key(row.getStudentMatriculationNumber()) + "|" + sectionKey, bulkRow.rowNumber());
            if (earlierRow != null) {
                report.reject(bulkRow, null, null, "Same enrollment as row " + earlierRow);
                continue;
            }
            Student student = studentsByMatric.get(BulkValues.key(row.getStudentMatriculationNumber()));
            if (student == null) {
                report.reject(bulkRow, "studentMatriculationNumber", row.getStudentMatriculationNumber(),
                        "No student has this matriculation number. Upload students first");
            }
            Section section = sectionsByKey.get(sectionKey);
            if (section == null) {
                report.reject(bulkRow, "sectionName", row.getSectionName(), "Course " + row.getCourseCode().trim()
                        + " has no section with this name in this academic period");
            }
            if (report.isRejected(bulkRow)) {
                continue;
            }

            if (!enrolled.add(student.getId() + "|" + section.getId())) {
                report.unchanged();
                continue;
            }
            Enrollment enrollment = new Enrollment();
            enrollment.setSchool(school);
            enrollment.setEnrollmentStudent(student);
            enrollment.setEnrollmentSection(section);
            toSave.add(enrollment);
            report.created();
        }

        enrollmentRepository.saveAll(toSave);
    }

    private static String sectionKey(String courseCode, String sectionName) {
        return courseCode.trim() + "|" + sectionName.trim();
    }
}
