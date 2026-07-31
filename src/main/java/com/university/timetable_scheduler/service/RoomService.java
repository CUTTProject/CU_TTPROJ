package com.university.timetable_scheduler.service;

import com.university.timetable_scheduler.dto.request.room.*;
import com.university.timetable_scheduler.dto.response.room.*;
import org.springframework.web.multipart.MultipartFile;

public interface RoomService {

    CreateRoomResponse createRoom(CreateRoomRequest request);

    ReadRoomResponse readRoom(ReadRoomRequest request);

    UpdateRoomResponse updateRoom(UpdateRoomRequest request);

    DeleteRoomResponse deleteRoom(DeleteRoomRequest request);

    BulkUploadRoomResponse bulkUploadRooms(MultipartFile file);

    BulkUploadRoomResponse bulkUploadRoomsArray(BulkUploadRoomArrayRequest request);

}

