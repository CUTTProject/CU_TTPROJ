package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.LecturerEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Lecturer extends TenantAwareEntity {

    private String lecturerStaffNumber;

    private String lecturerFirstName;

    private String lecturerLastName;

    private String lecturerEmail;

    @ManyToOne
    @JoinColumn(name = "lecturerDepartmentId")
    private Department lecturerDepartment;

    @Enumerated(EnumType.STRING)
    private LecturerEnum.LecturerStatus lecturerStatus;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        lecturerStatus = LecturerEnum.LecturerStatus.ACTIVE;
    }
}