package com.university.timetable_scheduler.dto.request.room;

import java.util.UUID;

import com.university.timetable_scheduler.status.RoomEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReadRoomRequest {
    private UUID id;
    private String roomBuilding;
    private String roomNumber;
    private Integer roomCapacity;
    private RoomEnum.RoomType roomType;
    private RoomEnum.RoomStatus roomStatus;
}

