package com.university.timetable_scheduler.dto.request.room;

import com.university.timetable_scheduler.status.RoomEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateRoomRequest {
    @NotBlank(message = "Room building is required")
    private String roomBuilding;

    @NotBlank(message = "Room number is required")
    private String roomNumber;

    @NotNull(message = "Room capacity is required")
    @Min(value = 1, message = "Room capacity must be at least 1")
    private Integer roomCapacity;

    @NotNull(message = "Room type is required")
    private RoomEnum.RoomType roomType;
}
