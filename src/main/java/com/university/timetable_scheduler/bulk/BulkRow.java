package com.university.timetable_scheduler.bulk;

/**
 * One data row of an upload, carrying the number it is reported under.
 *
 * @param rowNumber spreadsheet row for a CSV (first data row is 2), 1-based index for JSON
 */
public record BulkRow<R>(int rowNumber, R data) {
}
