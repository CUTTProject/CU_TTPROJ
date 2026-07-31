package com.university.timetable_scheduler.dto.request.room;

import com.university.timetable_scheduler.status.RoomEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class UpdateRoomRequest {
    @NotNull(message = "Room ID is required")
    private UUID id;

    private String roomBuilding;
    private String roomNumber;

    @Min(value = 1, message = "Room capacity must be at least 1")
    private Integer roomCapacity;

    private RoomEnum.RoomType roomType;
}
