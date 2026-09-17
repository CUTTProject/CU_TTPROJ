package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.ProgramEnum;
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
public class Program extends TenantAwareEntity {

    @Column(nullable = false)
    private String programName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programDepartmentId", nullable = false)
    private Department programDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programCoordinatorId")
    private Lecturer programCoordinator;

    @Enumerated(EnumType.STRING)
    private ProgramEnum.ProgramLevel programLevel;

    /** Length of the programme in years. */
    private Integer programDuration;

    @Column(length = 1000)
    private String programDescription;

    @Enumerated(EnumType.STRING)
    private ProgramEnum.ProgramStatus programStatus;

    /**
     * Unlike {@link Department}, the create form lets the user choose a status, so an
     * explicit value is honoured and ACTIVE is only a fallback.
     */
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (programStatus == null) {
            programStatus = ProgramEnum.ProgramStatus.ACTIVE;
        }
    }
}
