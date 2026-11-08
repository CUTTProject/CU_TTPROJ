package com.university.timetable_scheduler.status;

public class AcademicPeriodEnum {

    /**
     * The stored, hand-set switch. INACTIVE archives a period: it becomes read-only whatever its
     * dates say. Whether a period has started or ended is not stored here — see {@link AcademicPeriodPhase}.
     */
    public enum AcademicPeriodStatus {
        ACTIVE,
        INACTIVE
    }

    /**
     * Where a period stands, worked out from its dates and status at the moment of asking. Never
     * stored, so it is correct the instant a date passes — no scheduled job has to flip it, which
     * matters on a host that sleeps when idle.
     */
    public enum AcademicPeriodPhase {
        /** Before the start date. Open: schools set a period up before it begins. */
        UPCOMING,
        /** Between the start and end dates. Open. */
        IN_PROGRESS,
        /** Past the end date. Read-only until the end date is extended. */
        ENDED,
        /** Status set to INACTIVE by hand. Read-only until set back to ACTIVE. */
        ARCHIVED;

        public boolean isOpen() {
            return this == UPCOMING || this == IN_PROGRESS;
        }
    }
}
