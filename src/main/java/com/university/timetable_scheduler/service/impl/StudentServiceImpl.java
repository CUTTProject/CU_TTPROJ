package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.bulk.BulkRow;
import com.university.timetable_scheduler.bulk.BulkUploadOptions;
import com.university.timetable_scheduler.bulk.BulkUploadReport;
import com.university.timetable_scheduler.bulk.BulkUploadSupport;
import com.university.timetable_scheduler.bulk.BulkValues;
import com.university.timetable_scheduler.dto.request.student.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.student.*;
import com.university.timetable_scheduler.entity.Department;
import com.university.timetable_scheduler.entity.Program;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.entity.Student;
import com.university.timetable_scheduler.mapper.StudentMapper;
import com.university.timetable_scheduler.repository.DepartmentRepository;
import com.university.timetable_scheduler.repository.ProgramRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.repository.StudentRepository;
import com.university.timetable_scheduler.service.StudentService;
import com.university.timetable_scheduler.status.ActivityEnum;
import com.university.timetable_scheduler.status.BulkUploadEnum;
import com.university.timetable_scheduler.status.StudentEnum;
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
public class StudentServiceImpl implements StudentService {
    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final StudentMapper studentMapper;
    private final SchoolRepository schoolRepository;
    private final ActivityServiceImpl activityService;
    private final BulkUploadSupport bulkUploadSupport;

    private Program findProgram(java.util.UUID programId) {
        return programRepository.findByIdAndSchoolId(programId, TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));
    }

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
        if (request.getStudentProgramId() != null) {
            entity.setStudentProgram(findProgram(request.getStudentProgramId()));
        }
        Student saved = studentRepository.save(entity);
        activityService.record(ActivityEnum.ActivityType.STUDENT_CREATED, "New student added",
                ActivityServiceImpl.label(saved.getStudentFirstName(), saved.getStudentLastName()) + " was added");
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
                request.getStudentLevel(), request.getStudentDepartmentId(),
                request.getStudentProgramId(), request.getStudentStatus());
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
        if (request.getStudentProgramId() != null) {
            entity.setStudentProgram(findProgram(request.getStudentProgramId()));
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

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadStudents(MultipartFile file, boolean dryRun) {
        return bulkUploadSupport.importCsv(file, BulkUploadStudentArrayRequest.Row.class,
                BulkUploadOptions.of(BulkUploadEnum.BulkDataset.STUDENTS, dryRun), this::processStudentRows);
    }

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadStudentsArray(BulkUploadStudentArrayRequest request, boolean dryRun) {
        return bulkUploadSupport.importRows(request.getRows(),
                BulkUploadOptions.of(BulkUploadEnum.BulkDataset.STUDENTS, dryRun), this::processStudentRows);
    }

    /**
     * Upserts on {@code studentMatriculationNumber}. The department comes from the programme when
     * one is given, and from {@code departmentCode} otherwise. A blank programme leaves the
     * student's programme as it was, unless that programme is in a different department from the
     * row's, in which case it is cleared.
     */
    private void processStudentRows(List<BulkRow<BulkUploadStudentArrayRequest.Row>> rows, BulkUploadReport report) {
        School school = currentSchool();
        Map<String, Student> byMatric = BulkValues.index(studentRepository.findAllBySchool_Id(school.getId()),
                Student::getStudentMatriculationNumber);
        Map<String, Program> programsByCode =
                BulkValues.index(programRepository.findAllBySchool_Id(school.getId()), Program::getProgramCode);
        Map<String, Department> departmentsByCode =
                BulkValues.index(departmentRepository.findAllBySchool_Id(school.getId()), Department::getDepartmentCode);

        Map<String, Integer> firstRowByMatric = new HashMap<>();
        List<Student> toSave = new ArrayList<>();

        for (BulkRow<BulkUploadStudentArrayRequest.Row> bulkRow : rows) {
            BulkUploadStudentArrayRequest.Row row = bulkRow.data();
            String matric = BulkValues.key(row.getStudentMatriculationNumber());

            Integer earlierRow = firstRowByMatric.putIfAbsent(matric, bulkRow.rowNumber());
            if (earlierRow != null) {
                report.reject(bulkRow, "studentMatriculationNumber", row.getStudentMatriculationNumber(),
                        "Already given on row " + earlierRow + "; each student may appear once");
                continue;
            }
            StudentEnum.StudentLevel level = BulkValues.parseEnum(StudentEnum.StudentLevel.class,
                    row.getStudentLevel(), bulkRow, "studentLevel", report);

            Program program = null;
            if (!BulkValues.isBlank(row.getProgramCode())) {
                program = programsByCode.get(BulkValues.key(row.getProgramCode()));
                if (program == null) {
                    report.reject(bulkRow, "programCode", row.getProgramCode(), "No programme has this code");
                }
            }
            Department department = null;
            if (!BulkValues.isBlank(row.getDepartmentCode())) {
                department = departmentsByCode.get(BulkValues.key(row.getDepartmentCode()));
                if (department == null) {
                    report.reject(bulkRow, "departmentCode", row.getDepartmentCode(), "No department has this code");
                }
            }
            if (BulkValues.isBlank(row.getProgramCode()) && BulkValues.isBlank(row.getDepartmentCode())) {
                report.reject(bulkRow, null, null,
                        "Give a programCode, or a departmentCode for a student without a programme");
            }
            if (program != null && department != null && !sameId(program.getProgramDepartment(), department)) {
                report.reject(bulkRow, "departmentCode", row.getDepartmentCode(), "Programme "
                        + program.getProgramCode() + " belongs to department "
                        + program.getProgramDepartment().getDepartmentCode() + "; leave departmentCode blank or match it");
            }
            if (report.isRejected(bulkRow)) {
                continue;
            }

            Student student = byMatric.get(matric);
            boolean isNew = student == null;
            if (isNew) {
                student = new Student();
                student.setSchool(school);
                student.setStudentMatriculationNumber(row.getStudentMatriculationNumber().trim());
            }
            student.setStudentFirstName(row.getStudentFirstName().trim());
            student.setStudentLastName(row.getStudentLastName().trim());
            student.setStudentEmail(row.getStudentEmail().trim());
            student.setStudentLevel(level);

            if (program != null) {
                student.setStudentProgram(program);
                student.setStudentDepartment(program.getProgramDepartment());
            } else {
                student.setStudentDepartment(department);
                Program current = student.getStudentProgram();
                if (current != null && !sameId(current.getProgramDepartment(), department)) {
                    student.setStudentProgram(null);
                    report.warn(bulkRow, "programCode", null, "Programme " + current.getProgramCode()
                            + " was removed because it is not in department " + department.getDepartmentCode());
                }
            }

            toSave.add(student);
            if (isNew) report.created(); else report.updated();
        }

        studentRepository.saveAll(toSave);
    }

    /** By id, since a programme's department may be a lazy proxy rather than the loaded instance. */
    private static boolean sameId(Department a, Department b) {
        return a != null && b != null && a.getId().equals(b.getId());
    }
}
