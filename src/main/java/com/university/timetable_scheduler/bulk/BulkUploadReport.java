package com.university.timetable_scheduler.bulk;

import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Accumulates what happened to each row of one upload. */
@Getter
public class BulkUploadReport {
    private final List<BulkUploadResponse.RowIssue> errors = new ArrayList<>();
    private final List<BulkUploadResponse.RowIssue> warnings = new ArrayList<>();
    private final Set<Integer> rejectedRows = new HashSet<>();
    private int submitted;
    private int created;
    private int updated;
    private int unchanged;

    void submitted(int count) {
        submitted += count;
    }

    /** Marks the row as skipped. Safe to call more than once per row; it is counted once. */
    public void reject(int rowNumber, String column, Object value, String message) {
        rejectedRows.add(rowNumber);
        errors.add(new BulkUploadResponse.RowIssue(rowNumber, column, stringify(value), message));
    }

    public void reject(BulkRow<?> row, String column, Object value, String message) {
        reject(row.rowNumber(), column, value, message);
    }

    /** The row is still saved; only the named value was ignored. */
    public void warn(int rowNumber, String column, Object value, String message) {
        warnings.add(new BulkUploadResponse.RowIssue(rowNumber, column, stringify(value), message));
    }

    public void warn(BulkRow<?> row, String column, Object value, String message) {
        warn(row.rowNumber(), column, value, message);
    }

    public boolean isRejected(BulkRow<?> row) {
        return rejectedRows.contains(row.rowNumber());
    }

    public void created() {
        created++;
    }

    public void updated() {
        updated++;
    }

    public void unchanged() {
        unchanged++;
    }

    public int skipped() {
        return rejectedRows.size();
    }

    private static String stringify(Object value) {
        return value == null ? null : value.toString();
    }
}
