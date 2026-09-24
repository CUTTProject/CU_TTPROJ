package com.university.timetable_scheduler.bulk;

import com.university.timetable_scheduler.dto.request.section.BulkUploadSectionArrayRequest;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import jakarta.validation.Validation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The CSV half of BulkUploadSupport: headers, row numbers and cell conversion. */
class BulkUploadSupportCsvTest {

    private final BulkUploadSupport support = new BulkUploadSupport(
            JsonMapper.builder().build(),
            Validation.buildDefaultValidatorFactory().getValidator(),
            null, null);

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "upload.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private static final String HEADER =
            "courseCode,sectionName,sectionEnrollmentSize,lecturerStaffNumbers,eventDurationMinutes,eventType\n";

    @Test
    @DisplayName("rows are numbered as spreadsheet rows, counting blank lines but not reading them")
    void numbersRowsLikeASpreadsheet() {
        BulkUploadReport report = new BulkUploadReport();
        List<BulkRow<BulkUploadSectionArrayRequest.Row>> rows = support.readCsv(csv(HEADER
                + "CSC101,A,40,STF1,60,CLASS\n"
                + ",,,,,\n"
                + "CSC102,B,30,STF2/STF3,120,LAB\n"), BulkUploadSectionArrayRequest.Row.class, report);

        assertThat(rows).extracting(BulkRow::rowNumber).containsExactly(2, 4);
        assertThat(report.getSubmitted()).isEqualTo(2);
        BulkUploadSectionArrayRequest.Row second = rows.get(1).data();
        assertThat(second.getCourseCode()).isEqualTo("CSC102");
        assertThat(second.getLecturerStaffNumbers()).isEqualTo("STF2/STF3");
        assertThat(second.getEventDurationMinutes()).isEqualTo(120);
    }

    @Test
    @DisplayName("an Excel byte-order mark and header case do not hide columns")
    void toleratesBomAndHeaderCase() {
        BulkUploadReport report = new BulkUploadReport();
        List<BulkRow<BulkUploadSectionArrayRequest.Row>> rows = support.readCsv(csv("﻿"
                + "COURSECODE, SectionName ,lecturerstaffnumbers,eventDurationMinutes,eventType\n"
                + "CSC101,A,STF1,60,CLASS\n"), BulkUploadSectionArrayRequest.Row.class, report);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).data().getCourseCode()).isEqualTo("CSC101");
        assertThat(rows.get(0).data().getSectionName()).isEqualTo("A");
        assertThat(report.getWarnings()).isEmpty();
    }

    @Test
    @DisplayName("a missing required column fails the whole file and names the column")
    void missingRequiredColumnIsA400() {
        assertThatThrownBy(() -> support.readCsv(csv("courseCode,sectionName\nCSC101,A\n"),
                BulkUploadSectionArrayRequest.Row.class, new BulkUploadReport()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("lecturerStaffNumbers")
                .hasMessageContaining("eventDurationMinutes")
                .hasMessageContaining("eventType");
    }

    @Test
    @DisplayName("an unknown column is a header warning, not an error")
    void unknownColumnIsWarned() {
        BulkUploadReport report = new BulkUploadReport();
        support.readCsv(csv(HEADER.strip() + ",lecturerEmail\nCSC101,A,40,STF1,60,CLASS,x@y.z\n"),
                BulkUploadSectionArrayRequest.Row.class, report);

        assertThat(report.getWarnings()).singleElement().satisfies(w -> {
            assertThat(w.getRow()).isEqualTo(1);
            assertThat(w.getColumn()).isEqualTo("lecturerEmail");
        });
        assertThat(report.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("a non-numeric number rejects only its own row, naming the column")
    void badNumberRejectsOnlyThatRow() {
        BulkUploadReport report = new BulkUploadReport();
        List<BulkRow<BulkUploadSectionArrayRequest.Row>> rows = support.readCsv(csv(HEADER
                + "CSC101,A,40,STF1,sixty,CLASS\n"
                + "CSC102,B,30,STF2,60,LAB\n"), BulkUploadSectionArrayRequest.Row.class, report);

        assertThat(rows).extracting(BulkRow::rowNumber).containsExactly(3);
        BulkUploadResponse.RowIssue error = report.getErrors().get(0);
        assertThat(error.getRow()).isEqualTo(2);
        assertThat(error.getColumn()).isEqualTo("eventDurationMinutes");
        assertThat(error.getValue()).isEqualTo("sixty");
        assertThat(report.skipped()).isEqualTo(1);
    }
}
