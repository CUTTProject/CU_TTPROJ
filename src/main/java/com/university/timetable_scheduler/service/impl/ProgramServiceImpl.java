package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.bulk.BulkRow;
import com.university.timetable_scheduler.bulk.BulkUploadOptions;
import com.university.timetable_scheduler.bulk.BulkUploadReport;
import com.university.timetable_scheduler.bulk.BulkUploadSupport;
import com.university.timetable_scheduler.bulk.BulkValues;
import com.university.timetable_scheduler.dto.request.program.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.program.*;
import com.university.timetable_scheduler.entity.Department;
import com.university.timetable_scheduler.entity.Lecturer;
import com.university.timetable_scheduler.entity.Program;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.mapper.ProgramMapper;
import com.university.timetable_scheduler.repository.DepartmentRepository;
import com.university.timetable_scheduler.repository.LecturerRepository;
import com.university.timetable_scheduler.repository.ProgramRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.repository.StudentRepository;
import com.university.timetable_scheduler.service.ProgramService;
import com.university.timetable_scheduler.status.ActivityEnum;
import com.university.timetable_scheduler.status.BulkUploadEnum;
import com.university.timetable_scheduler.status.ProgramEnum;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProgramServiceImpl implements ProgramService {
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ProgramRepository programRepository;
    private final ProgramMapper programMapper;
    private final SchoolRepository schoolRepository;
    private final DepartmentRepository departmentRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;
    private final ActivityServiceImpl activityService;
    private final BulkUploadSupport bulkUploadSupport;

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    private Department findDepartment(UUID departmentId) {
        return departmentRepository.findByIdAndSchoolId(departmentId, TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
    }

    /** Any lecturer in the school may coordinate any programme; department is not enforced. */
    private Lecturer findCoordinator(UUID lecturerId) {
        return lecturerRepository.findByIdAndSchoolId(lecturerId, TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program coordinator lecturer not found"));
    }

    private static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    private void ensureCodeAvailable(String programCode, UUID excludeId) {
        if (programRepository.existsLiveByProgramCode(TenantContext.getSchoolId(), programCode, excludeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Program code '" + programCode + "' already exists");
        }
    }

    private void ensureNameAvailable(String programName, UUID departmentId, UUID excludeId) {
        if (programRepository.existsLiveByProgramNameInDepartment(
                TenantContext.getSchoolId(), programName, departmentId, excludeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Program '" + programName + "' already exists in this department");
        }
    }

    /**
     * Maps a page of programmes and attaches their student counts using a single grouped
     * query, rather than one count per row.
     */
    private List<ProgramResponse> withStudentCounts(List<Program> programs) {
        List<ProgramResponse> responses = programMapper.toResponseList(programs);
        if (responses.isEmpty()) {
            return responses;
        }
        List<UUID> ids = programs.stream().map(Program::getId).toList();
        Map<UUID, Long> counts = studentRepository
                .countLiveByProgramIds(TenantContext.getSchoolId(), ids).stream()
                .collect(Collectors.toMap(
                        StudentRepository.ProgramStudentCount::getProgramId,
                        StudentRepository.ProgramStudentCount::getStudentCount));
        responses.forEach(response -> response.setStudentCount(counts.getOrDefault(response.getId(), 0L)));
        return responses;
    }

    private ProgramResponse withStudentCount(Program program) {
        return withStudentCounts(List.of(program)).get(0);
    }

    @Override
    public CreateProgramResponse createProgram(CreateProgramRequest request) {
        Department department = findDepartment(request.getProgramDepartmentId());
        String programName = request.getProgramName().trim();
        ensureNameAvailable(programName, department.getId(), null);
        String programCode = BulkValues.isBlank(request.getProgramCode()) ? null : normalizeCode(request.getProgramCode());
        if (programCode != null) {
            ensureCodeAvailable(programCode, null);
        }

        Program entity = new Program();
        entity.setSchool(currentSchool());
        entity.setProgramName(programName);
        entity.setProgramCode(programCode);
        entity.setProgramDepartment(department);
        entity.setProgramCoordinator(findCoordinator(request.getProgramCoordinatorId()));
        entity.setProgramLevel(request.getProgramLevel());
        entity.setProgramDuration(request.getProgramDuration());
        entity.setProgramDescription(request.getProgramDescription());
        entity.setProgramStatus(request.getProgramStatus());
        Program saved = programRepository.save(entity);

        activityService.record(ActivityEnum.ActivityType.PROGRAM_CREATED, "New program added",
                ActivityServiceImpl.label(saved.getProgramName()) + " was added");

        CreateProgramResponse response = new CreateProgramResponse();
        CreateProgramResponse.Data responseData = new CreateProgramResponse.Data();
        responseData.setProgram(withStudentCount(saved));
        response.setData(responseData);
        return response;
    }

    @Override
    public ReadProgramResponse readProgram(ReadProgramRequest request) {
        int page = request.getPage() == null ? 0 : request.getPage();
        int size = request.getSize() == null ? DEFAULT_PAGE_SIZE : request.getSize();

        Page<Program> result = programRepository.findProgramByFilter(
                TenantContext.getSchoolId(),
                request.getId(), request.getProgramName(), request.getProgramDepartmentId(),
                request.getProgramLevel(), request.getProgramStatus(),
                PageRequest.of(page, size));

        ReadProgramResponse.Data responseData = new ReadProgramResponse.Data();
        responseData.setPrograms(withStudentCounts(result.getContent()));
        responseData.setPage(result.getNumber());
        responseData.setSize(result.getSize());
        responseData.setTotalElements(result.getTotalElements());
        responseData.setTotalPages(result.getTotalPages());

        ReadProgramResponse response = new ReadProgramResponse();
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public UpdateProgramResponse updateProgram(UpdateProgramRequest request) {
        Program entity = programRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        if (request.getProgramDepartmentId() != null) {
            entity.setProgramDepartment(findDepartment(request.getProgramDepartmentId()));
        }
        if (request.getProgramCoordinatorId() != null) {
            entity.setProgramCoordinator(findCoordinator(request.getProgramCoordinatorId()));
        }
        if (request.getProgramName() != null) {
            if (request.getProgramName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Program name cannot be blank");
            }
            request.setProgramName(request.getProgramName().trim());
        }
        if (request.getProgramCode() != null) {
            if (request.getProgramCode().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Program code cannot be blank");
            }
            request.setProgramCode(normalizeCode(request.getProgramCode()));
            ensureCodeAvailable(request.getProgramCode(), entity.getId());
        }
        // Checked against whichever department the programme ends up in, moved or not.
        String effectiveName = request.getProgramName() == null ? entity.getProgramName() : request.getProgramName();
        ensureNameAvailable(effectiveName, entity.getProgramDepartment().getId(), entity.getId());

        programMapper.updateDtoToEntity(request, entity);

        UpdateProgramResponse response = new UpdateProgramResponse();
        UpdateProgramResponse.Data responseData = new UpdateProgramResponse.Data();
        responseData.setProgram(withStudentCount(entity));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public DeleteProgramResponse deleteProgram(DeleteProgramRequest request) {
        Program entity = programRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));
        entity.setIsDeleted(true);
        return new DeleteProgramResponse();
    }

    @Override
    public ProgramStatsResponse readStats() {
        UUID schoolId = TenantContext.getSchoolId();

        ProgramStatsResponse.Data responseData = new ProgramStatsResponse.Data();
        responseData.setTotalPrograms(programRepository.countLiveBySchoolId(schoolId));
        responseData.setActivePrograms(
                programRepository.countLiveByStatus(schoolId, ProgramEnum.ProgramStatus.ACTIVE));
        responseData.setInactivePrograms(
                programRepository.countLiveByStatus(schoolId, ProgramEnum.ProgramStatus.INACTIVE));
        responseData.setDepartments(departmentRepository.countLiveBySchoolId(schoolId));

        ProgramStatsResponse response = new ProgramStatsResponse();
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadPrograms(MultipartFile file, boolean dryRun) {
        return bulkUploadSupport.importCsv(file, BulkUploadProgramArrayRequest.Row.class,
                BulkUploadOptions.of(BulkUploadEnum.BulkDataset.PROGRAMS, dryRun), this::processProgramRows);
    }

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadProgramsArray(BulkUploadProgramArrayRequest request, boolean dryRun) {
        return bulkUploadSupport.importRows(request.getPrograms(),
                BulkUploadOptions.of(BulkUploadEnum.BulkDataset.PROGRAMS, dryRun), this::processProgramRows);
    }

    /**
     * Upserts on {@code programCode}. A row whose code is new but whose name matches a programme
     * in the same department that has no code yet updates that programme, so programmes created
     * before codes existed are adopted instead of duplicated.
     */
    private void processProgramRows(List<BulkRow<BulkUploadProgramArrayRequest.Row>> rows, BulkUploadReport report) {
        School school = currentSchool();
        UUID schoolId = school.getId();

        Map<String, Department> departmentsByCode =
                BulkValues.index(departmentRepository.findAllBySchool_Id(schoolId), Department::getDepartmentCode);
        Map<String, Lecturer> lecturersByStaffNumber =
                BulkValues.index(lecturerRepository.findAllBySchool_Id(schoolId), Lecturer::getLecturerStaffNumber);

        List<Program> existing = programRepository.findAllBySchool_Id(schoolId);
        Map<String, Program> byCode = BulkValues.index(existing, Program::getProgramCode);
        Map<String, Program> byName = new HashMap<>();
        existing.stream()
                .filter(p -> p.getProgramName() != null && p.getProgramDepartment() != null)
                .forEach(p -> byName.putIfAbsent(programKey(p.getProgramName(), p.getProgramDepartment().getId()), p));

        Map<String, Integer> firstRowByCode = new HashMap<>();
        List<Program> toSave = new ArrayList<>();

        for (BulkRow<BulkUploadProgramArrayRequest.Row> bulkRow : rows) {
            BulkUploadProgramArrayRequest.Row row = bulkRow.data();
            String code = BulkValues.key(row.getProgramCode());

            Integer earlierRow = firstRowByCode.putIfAbsent(code, bulkRow.rowNumber());
            if (earlierRow != null) {
                report.reject(bulkRow, "programCode", row.getProgramCode(),
                        "Already given on row " + earlierRow + "; each programme may appear once");
                continue;
            }
            Department department = departmentsByCode.get(BulkValues.key(row.getDepartmentCode()));
            if (department == null) {
                report.reject(bulkRow, "departmentCode", row.getDepartmentCode(), "No department has this code");
            }
            ProgramEnum.ProgramLevel level = BulkValues.parseEnum(ProgramEnum.ProgramLevel.class,
                    row.getProgramLevel(), bulkRow, "programLevel", report);
            ProgramEnum.ProgramStatus status = BulkValues.parseEnum(ProgramEnum.ProgramStatus.class,
                    row.getProgramStatus(), bulkRow, "programStatus", report);
            if (report.isRejected(bulkRow)) {
                continue;
            }

            String name = row.getProgramName().trim();
            String nameKey = programKey(name, department.getId());
            Program program = byCode.get(code);
            if (program == null) {
                Program legacy = byName.get(nameKey);
                if (legacy != null && legacy.getProgramCode() == null) {
                    program = legacy;
                }
            }
            Program sameName = byName.get(nameKey);
            if (sameName != null && sameName != program) {
                report.reject(bulkRow, "programName", name, "Another programme in this department"
                        + (sameName.getProgramCode() == null ? "" : " (" + sameName.getProgramCode() + ")")
                        + " already has this name");
                continue;
            }

            boolean isNew = program == null;
            if (isNew) {
                program = new Program();
                program.setSchool(school);
            } else if (program.getProgramDepartment() != null) {
                byName.remove(programKey(program.getProgramName(), program.getProgramDepartment().getId()));
            }
            program.setProgramCode(code);
            program.setProgramName(name);
            program.setProgramDepartment(department);
            byCode.put(code, program);
            byName.put(nameKey, program);

            if (!BulkValues.isBlank(row.getProgramCoordinatorStaffNumber())) {
                Lecturer coordinator = lecturersByStaffNumber.get(BulkValues.key(row.getProgramCoordinatorStaffNumber()));
                if (coordinator == null) {
                    report.warn(bulkRow, "programCoordinatorStaffNumber", row.getProgramCoordinatorStaffNumber(),
                            "No lecturer has this staff number; coordinator left unchanged");
                } else {
                    program.setProgramCoordinator(coordinator);
                }
            }
            if (level != null) program.setProgramLevel(level);
            if (status != null) program.setProgramStatus(status);
            if (row.getProgramDuration() != null) program.setProgramDuration(row.getProgramDuration());
            if (BulkValues.text(row.getProgramDescription()) != null) {
                program.setProgramDescription(BulkValues.text(row.getProgramDescription()));
            }

            toSave.add(program);
            if (isNew) report.created(); else report.updated();
        }

        programRepository.saveAll(toSave);
    }

    private static String programKey(String programName, UUID departmentId) {
        return programName.trim().toUpperCase() + "|" + departmentId;
    }
}
