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

    /** Names are optional: a school may identify students by matriculation number alone. */
    private String studentFirstName;

    private String studentLastName;

    private String studentMatriculationNumber;

    private String studentEmail;

    @Enumerated(EnumType.STRING)
    private StudentEnum.StudentLevel studentLevel;

    @ManyToOne
    @JoinColumn(name = "studentDepartmentId")
    private Department studentDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studentProgramId")
    private Program studentProgram;

    @Enumerated(EnumType.STRING)
    private StudentEnum.StudentStatus studentStatus;

    /** The student's name if the school supplied one, otherwise their matriculation number. */
    public String displayName() {
        return PersonNames.displayName(studentMatriculationNumber, studentFirstName, studentLastName);
    }

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        studentStatus = StudentEnum.StudentStatus.ACTIVE;
    }
}