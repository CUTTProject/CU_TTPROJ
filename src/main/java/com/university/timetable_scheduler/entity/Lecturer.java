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

    /** Names are optional: a school may identify lecturers by staff number alone. */
    private String lecturerFirstName;

    private String lecturerLastName;

    private String lecturerEmail;

    @ManyToOne
    @JoinColumn(name = "lecturerDepartmentId")
    private Department lecturerDepartment;

    @Enumerated(EnumType.STRING)
    private LecturerEnum.LecturerStatus lecturerStatus;

    /** The lecturer's name if the school supplied one, otherwise their staff number. */
    public String displayName() {
        return PersonNames.displayName(lecturerStaffNumber, lecturerFirstName, lecturerLastName);
    }

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        lecturerStatus = LecturerEnum.LecturerStatus.ACTIVE;
    }
}