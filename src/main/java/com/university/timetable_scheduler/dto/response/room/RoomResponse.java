package com.university.timetable_scheduler.dto.response.room;

import java.util.UUID;

import com.university.timetable_scheduler.status.RoomEnum;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomResponse {
    private UUID id;
    private String roomBuilding;
    private String roomNumber;
    private Integer roomCapacity;
    private RoomEnum.RoomType roomType;
    private RoomEnum.RoomStatus roomStatus;
}

