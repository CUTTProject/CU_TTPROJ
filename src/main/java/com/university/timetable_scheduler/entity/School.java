package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.SchoolEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Time;
import java.time.LocalTime;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class School extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String schoolName;

    @Column(nullable = false, unique = true)
    private String schoolAdminEmail;

    @Column(nullable = false)
    private String schoolAdminPassword;

    @Column(nullable = false, unique = true)
    private String schoolAddress;

    @Column(nullable = false, unique = true)
    private String schoolPhone;

    @Column(nullable = false)
    private LocalTime schoolDayStartHour;

    @Column(nullable = false)
    private LocalTime schoolDayEndHour;

    @Enumerated(EnumType.STRING)
    private SchoolEnum.SchoolStatus schoolStatus;

    /**
     * Where generated timetables are POSTed. Optional; delivery is a no-op without one.
     *
     * <p>Nullable because {@code ddl-auto=update} adds this to a populated table. Length is explicit
     * because the 255 default is shorter than the 2000 the DTO and validator accept — a long URL
     * would validate, then fail to persist.
     */
    @Column(length = 2048)
    private String webhookUrl;

    /**
     * HMAC-SHA256 signing secret, stored in plaintext unlike {@link #schoolAdminPassword}.
     *
     * <p>Do not "fix" this to {@code passwordEncoder.encode(...)}: signing needs the raw value, so
     * hashing it compiles, passes every test short of an end-to-end signature check, and silently
     * breaks every delivery.
     *
     * <p>Treated as a credential elsewhere — shown once, absent from {@code SchoolResponse}, never
     * logged.
     */
    @Column
    private String webhookSecret;

    /**
     * Stops deliveries without discarding the URL and secret. Null reads as enabled, so rows
     * predating this column keep working.
     */
    @Column
    private Boolean webhookEnabled;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        schoolStatus = SchoolEnum.SchoolStatus.ACTIVE;
    }
}

