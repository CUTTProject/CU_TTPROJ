package com.university.timetable_scheduler.mapper;

import com.university.timetable_scheduler.dto.request.academicperiod.UpdateAcademicPeriodRequest;
import com.university.timetable_scheduler.dto.response.academicperiod.AcademicPeriodResponse;
import com.university.timetable_scheduler.entity.AcademicPeriod;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@code now} is passed in rather than read here, so the phase in a response comes from the same
 * clock that the write checks use, and tests can fix it.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AcademicPeriodMapper {

    @Mapping(target = "academicPeriodPhase", ignore = true)
    AcademicPeriodResponse toResponse(AcademicPeriod academicPeriod, @Context LocalDateTime now);

    List<AcademicPeriodResponse> toResponseList(List<AcademicPeriod> academicPeriods, @Context LocalDateTime now);

    @AfterMapping
    default void setPhase(AcademicPeriod academicPeriod, @MappingTarget AcademicPeriodResponse response,
                          @Context LocalDateTime now) {
        response.setAcademicPeriodPhase(academicPeriod.phaseAt(now));
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void updateDtoToEntity(UpdateAcademicPeriodRequest request, @MappingTarget AcademicPeriod academicPeriod);
}
