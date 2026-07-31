package com.university.timetable_scheduler.mapper;

import com.university.timetable_scheduler.dto.request.academicperiod.UpdateAcademicPeriodRequest;
import com.university.timetable_scheduler.dto.response.academicperiod.AcademicPeriodResponse;
import com.university.timetable_scheduler.entity.AcademicPeriod;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AcademicPeriodMapper {
    AcademicPeriodResponse toResponse(AcademicPeriod academicPeriod);
    List<AcademicPeriodResponse> toResponseList(List<AcademicPeriod> academicPeriods);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void updateDtoToEntity(UpdateAcademicPeriodRequest request, @MappingTarget AcademicPeriod academicPeriod);
}

