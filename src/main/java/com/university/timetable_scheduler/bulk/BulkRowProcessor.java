package com.university.timetable_scheduler.bulk;

import java.util.List;

/**
 * The dataset-specific half of an upload. Receives only rows that passed bean validation, and
 * must record exactly one outcome per row on the report: created, updated, unchanged, or rejected.
 */
@FunctionalInterface
public interface BulkRowProcessor<R> {
    void process(List<BulkRow<R>> rows, BulkUploadReport report);
}
