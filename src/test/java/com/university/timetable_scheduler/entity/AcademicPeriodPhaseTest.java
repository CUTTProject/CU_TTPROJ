package com.university.timetable_scheduler.entity;

import com.university.timetable_scheduler.status.AcademicPeriodEnum.AcademicPeriodPhase;
import com.university.timetable_scheduler.status.AcademicPeriodEnum.AcademicPeriodStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AcademicPeriodPhaseTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 14, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2027, 1, 29, 0, 0);

    private static AcademicPeriod period(AcademicPeriodStatus status, LocalDateTime start, LocalDateTime end) {
        AcademicPeriod p = new AcademicPeriod();
        p.setAcademicPeriodStatus(status);
        p.setAcademicPeriodStartDate(start);
        p.setAcademicPeriodEndDate(end);
        return p;
    }

    @Test
    @DisplayName("Before the start date a period is upcoming, and still open for setting up")
    void upcoming() {
        AcademicPeriodPhase phase = period(AcademicPeriodStatus.ACTIVE, START, END).phaseAt(START.minusSeconds(1));
        assertThat(phase).isEqualTo(AcademicPeriodPhase.UPCOMING);
        assertThat(phase.isOpen()).isTrue();
    }

    @Test
    @DisplayName("From the start date until the end date a period is in progress")
    void inProgress() {
        AcademicPeriod p = period(AcademicPeriodStatus.ACTIVE, START, END);
        assertThat(p.phaseAt(START)).isEqualTo(AcademicPeriodPhase.IN_PROGRESS);
        assertThat(p.phaseAt(END.minusSeconds(1))).isEqualTo(AcademicPeriodPhase.IN_PROGRESS);
    }

    @Test
    @DisplayName("At the end date the period is over, not a second later")
    void endsExactlyAtEndDate() {
        AcademicPeriod p = period(AcademicPeriodStatus.ACTIVE, START, END);
        assertThat(p.phaseAt(END)).isEqualTo(AcademicPeriodPhase.ENDED);
        assertThat(p.phaseAt(END.plusYears(1))).isEqualTo(AcademicPeriodPhase.ENDED);
        assertThat(AcademicPeriodPhase.ENDED.isOpen()).isFalse();
    }

    @Test
    @DisplayName("INACTIVE archives a period whatever its dates say")
    void archivedOverridesDates() {
        AcademicPeriod p = period(AcademicPeriodStatus.INACTIVE, START, END);
        assertThat(p.phaseAt(START.minusDays(1))).isEqualTo(AcademicPeriodPhase.ARCHIVED);
        assertThat(p.phaseAt(START.plusDays(1))).isEqualTo(AcademicPeriodPhase.ARCHIVED);
        assertThat(AcademicPeriodPhase.ARCHIVED.isOpen()).isFalse();
    }

    @Test
    @DisplayName("Missing dates never close a period, so older rows without them keep working")
    void missingDatesStayOpen() {
        assertThat(period(AcademicPeriodStatus.ACTIVE, null, null).phaseAt(END.plusYears(5)))
                .isEqualTo(AcademicPeriodPhase.IN_PROGRESS);
        assertThat(period(null, null, null).phaseAt(END)).isEqualTo(AcademicPeriodPhase.IN_PROGRESS);
    }
}
