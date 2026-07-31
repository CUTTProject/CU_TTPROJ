package com.university.timetable_scheduler.mapper;

import com.university.timetable_scheduler.dto.request.school.UpdateSchoolRequest;
import com.university.timetable_scheduler.dto.response.school.SchoolResponse;
import com.university.timetable_scheduler.entity.School;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SchoolMapper {
    SchoolResponse toResponse(School school);
    List<SchoolResponse> toResponseList(List<School> schools);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void updateDtoToEntity(UpdateSchoolRequest request, @MappingTarget School school);
}
