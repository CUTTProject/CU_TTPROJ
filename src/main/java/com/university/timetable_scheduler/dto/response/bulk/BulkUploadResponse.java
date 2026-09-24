package com.university.timetable_scheduler.dto.response.bulk;

import com.university.timetable_scheduler.dto.response.BaseResponse;
import com.university.timetable_scheduler.status.BulkUploadEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * The one response every bulk upload returns, CSV or JSON, whatever the dataset.
 *
 * <p>Uploads commit partially: valid rows are saved, and every row that was not is listed in
 * {@code errors} with the reason. A {@code dryRun} runs the same checks and reports the same
 * counts, then rolls back, so the client can show a preview before anything is written.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResponse extends BaseResponse {
    private Data data;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "BulkUploadResponseData")
    public static class Data {
        private BulkUploadEnum.BulkDataset dataset;
        /** True when nothing was saved: the counts are what a real upload would have done. */
        private boolean dryRun;
        /** Data rows received, excluding the CSV header and blank lines. */
        private int submitted;
        /** Rows that created a new record. */
        private int created;
        /** Rows that updated a record that already existed. */
        private int updated;
        /** Rows that matched an existing record exactly and changed nothing (enrollments). */
        private int unchanged;
        /** Rows that were rejected. Each has at least one entry in {@code errors}. */
        private int skipped;
        /** Why rows were rejected. A rejected row can have several. */
        private List<RowIssue> errors;
        /** Rows that were saved, but with a value ignored. Header-level warnings use row 1. */
        private List<RowIssue> warnings;
    }

    /**
     * One problem with one cell. {@code row} is the spreadsheet row number for a CSV (the header
     * is row 1, so the first data row is row 2) and the 1-based position in {@code rows} for JSON.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "BulkUploadRowIssue")
    public static class RowIssue {
        private int row;
        /** The column, or null when the problem is with the row as a whole. */
        private String column;
        private String value;
        private String message;
    }
}
