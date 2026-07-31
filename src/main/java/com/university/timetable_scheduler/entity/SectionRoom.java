package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.RoomEnum;
import com.university.timetable_scheduler.status.SectionRoomEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class SectionRoom extends TenantAwareEntity {
    @ManyToOne
    @JoinColumn(name = "sectionRoomId")
    private Room sectionRoom;

    @ManyToOne
    @JoinColumn(name = "sectionRoomSectionId")
    private Section sectionRoomSection;

    @Enumerated(EnumType.STRING)
    private SectionRoomEnum.SectionRoomStatus sectionRoomStatus;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        sectionRoomStatus = SectionRoomEnum.SectionRoomStatus.ACTIVE;
    }
}
