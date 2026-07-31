package com.university.timetable_scheduler.mapper;

import com.university.timetable_scheduler.dto.request.timeslot.UpdateTimeslotRequest;
import com.university.timetable_scheduler.dto.response.timeslot.TimeslotResponse;
import com.university.timetable_scheduler.entity.Timeslot;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TimeslotMapper {
    TimeslotResponse toResponse(Timeslot timeslot);
    List<TimeslotResponse> toResponseList(List<Timeslot> timeslots);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void updateDtoToEntity(UpdateTimeslotRequest request, @MappingTarget Timeslot timeslot);
}

