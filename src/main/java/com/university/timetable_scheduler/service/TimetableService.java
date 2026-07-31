package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.timetable.BulkUploadTimetableArrayRequest;
import com.university.timetable_scheduler.dto.request.timetable.DownloadConflictGraphRequest;
import com.university.timetable_scheduler.dto.request.timetable.DownloadTimetableRequest;
import com.university.timetable_scheduler.dto.request.timetable.GenerateTimetableRequest;
import com.university.timetable_scheduler.dto.response.timetable.BulkUploadTimetableResponse;
import com.university.timetable_scheduler.dto.response.timetable.TimetableResponse;
import com.university.timetable_scheduler.entity.Room;
import com.university.timetable_scheduler.entity.Timeslot;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TimetableService {
    /**
     * The result of assigning an event: a contiguous block of timeslots
     * (e.g. two back-to-back slots for a 2-hour event) and a room.
     */
    record EventAssignment(List<Timeslot> timeslots, Room room) {}

    /**
     * Run the ACO + min-conflicts solver over one academic period, persist the results, and return
     * the final assignment map (eventId → EventAssignment).
     *
     * <p>The period is a required argument rather than an ambient default: the previous signature
     * took none and the implementation loaded every event in the school, so scheduling one period
     * silently rescheduled all the others.
     */
    Map<UUID, EventAssignment> solve(UUID academicPeriodId);

    /**
     * Generate the timetable by running the solver and returning a structured
     * list of scheduled entries (JSON representation).
     */
    TimetableResponse generateTimetable(GenerateTimetableRequest generateTimetableRequest);

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
