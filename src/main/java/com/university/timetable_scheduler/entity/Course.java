package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.CourseEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
@Setter
@AllArgsConstructor
public class Course extends TenantAwareEntity {

    @Column(length = 20, nullable = false)
    private String courseCode;

    @Column(length = 250)
    private String courseName;

    @Column(length = 1000)
    private String courseDescription;

    private Integer courseUnit;

    @Enumerated(EnumType.STRING)
    private CourseEnum.CourseLevel courseLevel;

    @Enumerated(EnumType.STRING)
    private CourseEnum.CourseStatus courseStatus;

    @PrePersist
    protected void onCreate () {
        super.onCreate();
        courseStatus = CourseEnum.CourseStatus.ACTIVE;
    }
}
