package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.SchoolEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Time;
import java.time.LocalTime;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class School extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String schoolName;

    @Column(nullable = false, unique = true)
    private String schoolAdminEmail;

    @Column(nullable = false)
    private String schoolAdminPassword;

    @Column(nullable = false, unique = true)
    private String schoolAddress;

    @Column(nullable = false, unique = true)
    private String schoolPhone;

    @Column(nullable = false)
    private LocalTime schoolDayStartHour;

    @Column(nullable = false)
    private LocalTime schoolDayEndHour;

    @Enumerated(EnumType.STRING)
    private SchoolEnum.SchoolStatus schoolStatus;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        schoolStatus = SchoolEnum.SchoolStatus.ACTIVE;
    }
}

