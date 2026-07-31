package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.DepartmentEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Department extends TenantAwareEntity {

    private String departmentName;

    @Enumerated(EnumType.STRING)
    private DepartmentEnum.DepartmentStatus departmentStatus;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        departmentStatus = DepartmentEnum.DepartmentStatus.ACTIVE;
    }
}
