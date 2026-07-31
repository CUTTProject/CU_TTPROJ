package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.RoomEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Room extends TenantAwareEntity {

    private String roomBuilding;

    private String roomNumber;

    private Integer roomCapacity;

    @Enumerated(EnumType.STRING)
    private RoomEnum.RoomType roomType;

    @Enumerated(EnumType.STRING)
    private RoomEnum.RoomStatus roomStatus;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        roomStatus = RoomEnum.RoomStatus.ACTIVE;
    }
}
