package com.university.timetable_scheduler.dto.request.room;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * JSON body for the array-based room bulk-upload endpoint.
 * Each row mirrors the fields of the CSV version ({@link BulkUploadRoomFileRequest}).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BulkUploadRoomArrayRequest {

    @NotEmpty(message = "rooms must not be empty")
    @Valid
    private List<Row> rooms;

    @Schema(name = "BulkRoomRow")
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Row {

        private String roomBuilding;

        @NotBlank(message = "roomNumber is required")
        private String roomNumber;

        @Min(value = 1, message = "roomCapacity must be at least 1")
        private Integer roomCapacity;

        /** Must match RoomEnum.RoomType: LAB | SEMINAR_ROOM | LECTURE_THEATRE */
        private String roomType;
    }
}

