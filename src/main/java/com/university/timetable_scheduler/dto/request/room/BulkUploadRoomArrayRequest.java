package com.university.timetable_scheduler.dto.request.room;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Room bulk upload. Upserts on {@code roomNumber + roomBuilding}.
 *
 * <p>Rows are deliberately not {@code @Valid}-cascaded: BulkUploadSupport validates them one at
 * a time, so a bad row is reported and skipped instead of rejecting the whole request.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BulkUploadRoomArrayRequest {

    @NotEmpty(message = "rooms must not be empty")
    private List<Row> rooms;

    /** The field names are the CSV column names. */
    @Schema(name = "BulkRoomRow")
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Row {

        @NotBlank(message = "roomNumber is required")
        private String roomNumber;

        private String roomBuilding;

        @Min(value = 1, message = "roomCapacity must be at least 1")
        private Integer roomCapacity;

        /** RoomEnum.RoomType: CLASS | LAB | SEMINAR_ROOM | LECTURE_THEATRE */
        private String roomType;
    }
}
