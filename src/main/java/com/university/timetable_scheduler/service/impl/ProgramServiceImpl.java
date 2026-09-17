package com.university.timetable_scheduler.service.impl;

import com.opencsv.bean.CsvToBeanBuilder;
import com.university.timetable_scheduler.dto.request.program.*;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
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

        Program entity = new Program();
        entity.setSchool(currentSchool());
        entity.setProgramName(programName);
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
    public BulkUploadProgramResponse bulkUploadPrograms(MultipartFile file) {
        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            List<BulkUploadProgramFileRequest> rows =
                    new CsvToBeanBuilder<BulkUploadProgramFileRequest>(reader)
                            .withType(BulkUploadProgramFileRequest.class)
                            .withIgnoreLeadingWhiteSpace(true)
                            .build()
                            .parse();

            UploadOutcome outcome = processProgramRows(rows.stream().map(r -> {
                BulkUploadProgramArrayRequest.Row row = new BulkUploadProgramArrayRequest.Row();
                row.setProgramName(r.getProgramName());
                row.setDepartmentCode(r.getDepartmentCode());
                row.setProgramCoordinatorStaffNumber(r.getProgramCoordinatorStaffNumber());
                row.setProgramLevel(r.getProgramLevel());
                row.setProgramDuration(r.getProgramDuration());
                row.setProgramDescription(r.getProgramDescription());
                row.setProgramStatus(r.getProgramStatus());
                return row;
            }).toList(), currentSchool());

            return uploadResponse(outcome, rows.size());

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Program bulk upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public BulkUploadProgramResponse bulkUploadProgramsArray(BulkUploadProgramArrayRequest request) {
        UploadOutcome outcome = processProgramRows(request.getPrograms(), currentSchool());
        return uploadResponse(outcome, request.getPrograms().size());
    }

    private BulkUploadProgramResponse uploadResponse(UploadOutcome outcome, int submitted) {
        activityService.record(ActivityEnum.ActivityType.PROGRAMS_UPLOADED, "Program data uploaded successfully",
                ActivityServiceImpl.imported(outcome.imported(), "program"));

        BulkUploadProgramResponse response = new BulkUploadProgramResponse();
        response.setError(false);
        response.setResponseCode("200");
        String message = "Upload complete. " + submitted + " program(s) processed.";
        if (outcome.skipped() > 0) {
            message += " " + outcome.skipped() + " row(s) skipped: unknown department code or missing name.";
        }
        response.setResponseMessage(message);
        return response;
    }

    /**
     * Upserts on name-within-department, the same identity rule {@code /create} enforces.
     * A row naming a department that does not exist is skipped and counted rather than
     * failing the whole batch, so one bad line cannot cost the operator the upload.
     */
    private UploadOutcome processProgramRows(List<BulkUploadProgramArrayRequest.Row> rows, School school) {
        UUID schoolId = school.getId();

        Map<String, Department> departmentsByCode = departmentRepository.findAllBySchool_Id(schoolId).stream()
                .filter(d -> d.getDepartmentCode() != null && !d.getDepartmentCode().isBlank())
                .collect(Collectors.toMap(d -> d.getDepartmentCode().trim().toUpperCase(),
                        Function.identity(), (first, duplicate) -> first));

        Map<String, Lecturer> lecturersByStaffNumber = lecturerRepository.findAllBySchool_Id(schoolId).stream()
                .filter(l -> l.getLecturerStaffNumber() != null && !l.getLecturerStaffNumber().isBlank())
                .collect(Collectors.toMap(l -> l.getLecturerStaffNumber().trim().toUpperCase(),
                        Function.identity(), (first, duplicate) -> first));

        Map<String, Program> cache = new HashMap<>();
        programRepository.findAllBySchool_Id(schoolId).forEach(p -> {
            if (p.getProgramName() != null && p.getProgramDepartment() != null) {
                cache.put(programKey(p.getProgramName(), p.getProgramDepartment().getId()), p);
            }
        });

        List<Program> toSave = new ArrayList<>();
        int skipped = 0;

        for (BulkUploadProgramArrayRequest.Row row : rows) {
            if (row.getProgramName() == null || row.getProgramName().isBlank()
                    || row.getDepartmentCode() == null || row.getDepartmentCode().isBlank()) {
                skipped++;
                continue;
            }
            Department department = departmentsByCode.get(row.getDepartmentCode().trim().toUpperCase());
            if (department == null) {
                skipped++;
                continue;
            }

            String cacheKey = programKey(row.getProgramName(), department.getId());
            Program program = cache.getOrDefault(cacheKey, new Program());
            program.setSchool(school);
            program.setProgramName(row.getProgramName().trim());
            program.setProgramDepartment(department);

            if (row.getProgramCoordinatorStaffNumber() != null && !row.getProgramCoordinatorStaffNumber().isBlank()) {
                Lecturer coordinator = lecturersByStaffNumber.get(
                        row.getProgramCoordinatorStaffNumber().trim().toUpperCase());
                if (coordinator != null) {
                    program.setProgramCoordinator(coordinator);
                }
            }
            if (row.getProgramLevel() != null && !row.getProgramLevel().isBlank()) {
                try {
                    program.setProgramLevel(ProgramEnum.ProgramLevel.valueOf(row.getProgramLevel().trim().toUpperCase()));
                } catch (IllegalArgumentException ignore) {}
            }
            if (row.getProgramStatus() != null && !row.getProgramStatus().isBlank()) {
                try {
                    program.setProgramStatus(ProgramEnum.ProgramStatus.valueOf(row.getProgramStatus().trim().toUpperCase()));
                } catch (IllegalArgumentException ignore) {}
            }
            if (row.getProgramDuration() != null) {
                program.setProgramDuration(row.getProgramDuration());
            }
            if (row.getProgramDescription() != null && !row.getProgramDescription().isBlank()) {
                program.setProgramDescription(row.getProgramDescription().trim());
            }

            toSave.add(program);
            cache.put(cacheKey, program);
        }

        programRepository.saveAll(toSave);
        // A programme repeated in the upload is the same object twice; Program has identity equality.
        return new UploadOutcome((int) toSave.stream().distinct().count(), skipped);
    }

    private static String programKey(String programName, UUID departmentId) {
        return programName.trim().toUpperCase() + "|" + departmentId;
    }

    private record UploadOutcome(int imported, int skipped) {}
}
