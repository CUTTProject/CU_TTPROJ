package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.EnrollmentEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Enrollment extends TenantAwareEntity {
    @ManyToOne
    @JoinColumn(name = "enrollmentStudentId")
    private Student enrollmentStudent;

    @ManyToOne
    @JoinColumn(name = "enrollmentSectionId")
    private Section enrollmentSection;

    @Enumerated(EnumType.STRING)
    private EnrollmentEnum.EnrollmentStatus enrollmentStatus;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        enrollmentStatus = EnrollmentEnum.EnrollmentStatus.ACTIVE;
    }
}
