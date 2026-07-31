package com.university.timetable_scheduler.mapper;

import com.university.timetable_scheduler.dto.request.room.UpdateRoomRequest;
import com.university.timetable_scheduler.dto.response.room.RoomResponse;
import com.university.timetable_scheduler.entity.Room;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RoomMapper {
    RoomResponse toResponse(Room room);
    List<RoomResponse> toResponseList(List<Room> rooms);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void updateDtoToEntity(UpdateRoomRequest request, @MappingTarget Room room);
}

