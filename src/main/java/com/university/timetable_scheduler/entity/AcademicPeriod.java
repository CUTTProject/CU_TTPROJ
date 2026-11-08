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

    /**
     * The phase at {@code now}. The end date is the moment the period closes: at or after it, the
     * period has ended. Dates are compared as stored, against the application clock (UTC).
     */
    public AcademicPeriodEnum.AcademicPeriodPhase phaseAt(LocalDateTime now) {
        if (academicPeriodStatus == AcademicPeriodEnum.AcademicPeriodStatus.INACTIVE) {
            return AcademicPeriodEnum.AcademicPeriodPhase.ARCHIVED;
        }
        if (academicPeriodEndDate != null && !now.isBefore(academicPeriodEndDate)) {
            return AcademicPeriodEnum.AcademicPeriodPhase.ENDED;
        }
        if (academicPeriodStartDate != null && now.isBefore(academicPeriodStartDate)) {
            return AcademicPeriodEnum.AcademicPeriodPhase.UPCOMING;
        }
        return AcademicPeriodEnum.AcademicPeriodPhase.IN_PROGRESS;
    }

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        academicPeriodStatus = AcademicPeriodEnum.AcademicPeriodStatus.ACTIVE;
    }
}