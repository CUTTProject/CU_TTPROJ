package com.university.timetable_scheduler.dto.request.room;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class BulkUploadRoomFileRequest {

    @CsvBindByName(column = "roomBuilding", required = true)
    private String roomBuilding;

    @CsvBindByName(column = "roomNumber", required = true)
    private String roomNumber;

    @CsvBindByName(column = "roomCapacity", required = false)
    private Integer roomCapacity;

    @CsvBindByName(column = "roomType", required = false)
    private String roomType;
}

