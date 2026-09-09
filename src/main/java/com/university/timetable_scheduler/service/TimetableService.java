package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.timetable.BulkUploadTimetableArrayRequest;
import com.university.timetable_scheduler.dto.request.timetable.DownloadConflictGraphRequest;
import com.university.timetable_scheduler.dto.request.timetable.DownloadTimetableRequest;
import com.university.timetable_scheduler.dto.response.timetable.BulkUploadTimetableResponse;
import com.university.timetable_scheduler.entity.Room;
import com.university.timetable_scheduler.entity.Timeslot;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface TimetableService {
    /**
     * The result of assigning an event: a contiguous block of timeslots
     * (e.g. two back-to-back slots for a 2-hour event) and a room.
     */
    record EventAssignment(List<Timeslot> timeslots, Room room) {}

    /**
     * An assignment reduced to ids — what crosses the transaction boundary.
     *
     * <p>The solver runs untransacted, so its entities are detached by write-back time. Passing them
     * to {@code saveAll} would merge a stale snapshot over the row and revert anything edited
     * meanwhile, so only ids cross and the rows are re-read fresh.
     *
     * <p>{@code timeslotId} is the <em>first</em> slot of the block.
     */
    record AssignmentIds(UUID timeslotId, UUID roomId) {}

    /**
     * Generate and return the timetable as a PDF byte array.
     * Reads from already-persisted solver results in the DB.
     */
    byte[] downloadTimetablePdf(DownloadTimetableRequest downloadTimetableRequest);

    /**
     * Returns a Graphviz DOT string representing the section conflict graph.
     * Red edges = SAME_LECTURER, orange edges = OVERLAPPING_STUDENTS.
     */
    String getConflictGraphDot(DownloadConflictGraphRequest downloadConflictGraphRequest);

    BulkUploadTimetableResponse bulkUploadTimetable(MultipartFile file, UUID academicPeriodId);

    BulkUploadTimetableResponse bulkUploadTimetableArray(BulkUploadTimetableArrayRequest request);
}
