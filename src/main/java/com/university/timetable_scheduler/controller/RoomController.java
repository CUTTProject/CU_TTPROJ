package com.university.timetable_scheduler.controller;

import com.university.timetable_scheduler.dto.request.room.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.room.*;
import com.university.timetable_scheduler.service.impl.RoomServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/rooms")
@AllArgsConstructor
public class RoomController {
    private final RoomServiceImpl roomService;

    @PostMapping("/create")
    public CreateRoomResponse createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return roomService.createRoom(request);
    }

    @GetMapping("/read")
    public ReadRoomResponse readRoom(@Valid @ModelAttribute ReadRoomRequest request) {
        return roomService.readRoom(request);
    }

    @PutMapping("/update")
    public UpdateRoomResponse updateRoom(@Valid @RequestBody UpdateRoomRequest request) {
        return roomService.updateRoom(request);
    }

    @DeleteMapping("/delete")
    public DeleteRoomResponse deleteRoom(@Valid @ModelAttribute DeleteRoomRequest request) {
        return roomService.deleteRoom(request);
    }

    @Operation(summary = "Bulk upload rooms from a CSV file. Columns: roomNumber, roomBuilding, roomCapacity, "
            + "roomType (CLASS | LAB | SEMINAR_ROOM | LECTURE_THEATRE). Upserts by roomNumber + "
            + "roomBuilding. Valid rows are saved; rejected rows are listed with the reason. Pass "
            + "dryRun=true to validate and see the counts without saving.")
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BulkUploadResponse bulkUploadRooms(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return roomService.bulkUploadRooms(file, dryRun);
    }

    @Operation(summary = "Bulk upload rooms from a JSON array. Rows use the CSV column names. Pass dryRun=true to "
            + "validate and see the counts without saving.")
    @PostMapping(value = "/bulk-upload/array", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BulkUploadResponse bulkUploadRoomsArray(
            @Valid @RequestBody BulkUploadRoomArrayRequest request,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return roomService.bulkUploadRoomsArray(request, dryRun);
    }
}
