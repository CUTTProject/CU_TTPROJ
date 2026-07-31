package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.dto.request.school.*;
import com.university.timetable_scheduler.dto.response.school.*;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.mapper.SchoolMapper;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.service.SchoolService;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class SchoolServiceImpl implements SchoolService {

    private final SchoolRepository schoolRepository;
    private final SchoolMapper schoolMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public CreateSchoolResponse createSchool(CreateSchoolRequest request) {
        School entity = new School();
        entity.setSchoolName(request.getSchoolName());
        entity.setSchoolAddress(request.getSchoolAddress());
        entity.setSchoolAdminEmail(request.getSchoolAdminEmail());
        entity.setSchoolAdminPassword(passwordEncoder.encode(request.getSchoolAdminPassword()));
        entity.setSchoolPhone(request.getSchoolPhone());
        entity.setSchoolDayStartHour(request.getSchoolDayStartHour());
        entity.setSchoolDayEndHour(request.getSchoolDayEndHour());
        School saved = schoolRepository.save(entity);
        CreateSchoolResponse response = new CreateSchoolResponse();
        CreateSchoolResponse.Data data = new CreateSchoolResponse.Data();
        data.setSchool(schoolMapper.toResponse(saved));
        response.setData(data);
        return response;
    }

    /**
     * A school may only ever address itself. The caller's id comes from the JWT, never from the
     * request body, so an id naming another tenant resolves to nothing rather than to that tenant.
     */
    private School requireOwnSchool(UUID requestedId) {
        UUID schoolId = TenantContext.getSchoolId();
        if (schoolId == null || (requestedId != null && !schoolId.equals(requestedId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found");
        }
        return schoolRepository.findLiveById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));
    }

    @Override
    public ReadSchoolResponse readSchool(ReadSchoolRequest request) {
        UUID schoolId = TenantContext.getSchoolId();
        List<School> list = (request.getId() != null && !request.getId().equals(schoolId))
                ? List.of()
                : schoolRepository.findSchoolByFilter(
                        schoolId, request.getSchoolName(), request.getSchoolStatus());
        ReadSchoolResponse response = new ReadSchoolResponse();
        ReadSchoolResponse.Data data = new ReadSchoolResponse.Data();
        data.setSchools(schoolMapper.toResponseList(list));
        response.setData(data);
        return response;
    }

    @Override
    @Transactional
    public UpdateSchoolResponse updateSchool(UpdateSchoolRequest request) {
        School entity = requireOwnSchool(request.getId());
        schoolMapper.updateDtoToEntity(request, entity);
        UpdateSchoolResponse response = new UpdateSchoolResponse();
        UpdateSchoolResponse.Data data = new UpdateSchoolResponse.Data();
        data.setSchool(schoolMapper.toResponse(entity));
        response.setData(data);
        return response;
    }

    @Override
    @Transactional
    public DeleteSchoolResponse deleteSchool(DeleteSchoolRequest request) {
        School entity = requireOwnSchool(request.getId());
        entity.setIsDeleted(true);
        return new DeleteSchoolResponse();
    }
}
