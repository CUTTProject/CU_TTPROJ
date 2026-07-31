package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.AcademicPeriodEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class AcademicPeriod extends TenantAwareEntity {

    private String academicPeriodName;

    private String academicPeriodSession;

    private String academicPeriodSemester;

    private LocalDateTime academicPeriodStartDate;

    private LocalDateTime academicPeriodEndDate;

    @Enumerated(EnumType.STRING)
    private AcademicPeriodEnum.AcademicPeriodStatus academicPeriodStatus;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        academicPeriodStatus = AcademicPeriodEnum.AcademicPeriodStatus.ACTIVE;
    }
}