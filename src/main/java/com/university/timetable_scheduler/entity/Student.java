package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.StudentEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Student extends TenantAwareEntity {

    private String studentFirstName;

    private String studentLastName;

    private String studentMatriculationNumber;

    private String studentEmail;

    @Enumerated(EnumType.STRING)
    private StudentEnum.StudentLevel studentLevel;

    @ManyToOne
    @JoinColumn(name = "studentDepartmentId")
    private Department studentDepartment;

    @Enumerated(EnumType.STRING)
    private StudentEnum.StudentStatus studentStatus;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        studentStatus = StudentEnum.StudentStatus.ACTIVE;
    }
}