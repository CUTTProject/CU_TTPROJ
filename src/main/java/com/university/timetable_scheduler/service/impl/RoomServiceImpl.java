package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.bulk.BulkRow;
import com.university.timetable_scheduler.bulk.BulkUploadOptions;
import com.university.timetable_scheduler.bulk.BulkUploadReport;
import com.university.timetable_scheduler.bulk.BulkUploadSupport;
import com.university.timetable_scheduler.bulk.BulkValues;
import com.university.timetable_scheduler.dto.request.room.*;
import com.university.timetable_scheduler.dto.response.bulk.BulkUploadResponse;
import com.university.timetable_scheduler.dto.response.room.*;
import com.university.timetable_scheduler.entity.Room;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.mapper.RoomMapper;
import com.university.timetable_scheduler.repository.RoomRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.service.RoomService;
import com.university.timetable_scheduler.status.ActivityEnum;
import com.university.timetable_scheduler.status.BulkUploadEnum;
import com.university.timetable_scheduler.status.RoomEnum;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class RoomServiceImpl implements RoomService {
    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;
    private final SchoolRepository schoolRepository;
    private final ActivityServiceImpl activityService;
    private final BulkUploadSupport bulkUploadSupport;

    private School currentSchool() {
        return schoolRepository.findLiveById(TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid school context"));
    }

    @Override
    public CreateRoomResponse createRoom(CreateRoomRequest request) {
        Room entity = new Room();
        entity.setSchool(currentSchool());
        entity.setRoomBuilding(request.getRoomBuilding());
        entity.setRoomNumber(request.getRoomNumber());
        entity.setRoomCapacity(request.getRoomCapacity());
        entity.setRoomType(request.getRoomType());
        Room saved = roomRepository.save(entity);
        activityService.record(ActivityEnum.ActivityType.ROOM_CREATED, "New room added",
                ActivityServiceImpl.label(saved.getRoomBuilding(), saved.getRoomNumber()) + " was added");
        CreateRoomResponse response = new CreateRoomResponse();
        CreateRoomResponse.Data responseData = new CreateRoomResponse.Data();
        responseData.setRoom(roomMapper.toResponse(saved));
        response.setData(responseData);
        return response;
    }

    @Override
    public ReadRoomResponse readRoom(ReadRoomRequest request) {
        List<Room> list = roomRepository.findRoomByFilter(
                TenantContext.getSchoolId(),
                request.getId(), request.getRoomBuilding(), request.getRoomNumber(),
                request.getRoomCapacity(), request.getRoomType(), request.getRoomStatus());
        ReadRoomResponse response = new ReadRoomResponse();
        ReadRoomResponse.Data responseData = new ReadRoomResponse.Data();
        responseData.setRooms(roomMapper.toResponseList(list));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public UpdateRoomResponse updateRoom(UpdateRoomRequest request) {
        Room entity = roomRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        roomMapper.updateDtoToEntity(request, entity);
        UpdateRoomResponse response = new UpdateRoomResponse();
        UpdateRoomResponse.Data responseData = new UpdateRoomResponse.Data();
        responseData.setRoom(roomMapper.toResponse(entity));
        response.setData(responseData);
        return response;
    }

    @Override
    @Transactional
    public DeleteRoomResponse deleteRoom(DeleteRoomRequest request) {
        Room entity = roomRepository.findByIdAndSchoolId(request.getId(), TenantContext.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        entity.setIsDeleted(true);
        return new DeleteRoomResponse();
    }

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadRooms(MultipartFile file, boolean dryRun) {
        return bulkUploadSupport.importCsv(file, BulkUploadRoomArrayRequest.Row.class,
                BulkUploadOptions.of(BulkUploadEnum.BulkDataset.ROOMS, dryRun), this::processRoomRows);
    }

    @Override
    @Transactional
    public BulkUploadResponse bulkUploadRoomsArray(BulkUploadRoomArrayRequest request, boolean dryRun) {
        return bulkUploadSupport.importRows(request.getRooms(),
                BulkUploadOptions.of(BulkUploadEnum.BulkDataset.ROOMS, dryRun), this::processRoomRows);
    }

    /** Upserts on room number within building; a blank building is its own building. */
    private void processRoomRows(List<BulkRow<BulkUploadRoomArrayRequest.Row>> rows, BulkUploadReport report) {
        School school = currentSchool();
        Map<String, Room> existing = BulkValues.index(roomRepository.findAllBySchool_Id(school.getId()),
                r -> r.getRoomNumber() == null ? null : roomKey(r.getRoomNumber(), r.getRoomBuilding()));

        Map<String, Integer> firstRowByKey = new HashMap<>();
        List<Room> toSave = new ArrayList<>();

        for (BulkRow<BulkUploadRoomArrayRequest.Row> bulkRow : rows) {
            BulkUploadRoomArrayRequest.Row row = bulkRow.data();
            String key = BulkValues.key(roomKey(row.getRoomNumber(), row.getRoomBuilding()));

            Integer earlierRow = firstRowByKey.putIfAbsent(key, bulkRow.rowNumber());
            if (earlierRow != null) {
                report.reject(bulkRow, "roomNumber", row.getRoomNumber(),
                        "Already given on row " + earlierRow + "; each room may appear once");
                continue;
            }
            RoomEnum.RoomType roomType = BulkValues.parseEnum(RoomEnum.RoomType.class,
                    row.getRoomType(), bulkRow, "roomType", report);
            if (report.isRejected(bulkRow)) {
                continue;
            }

            Room room = existing.get(key);
            boolean isNew = room == null;
            if (isNew) {
                room = new Room();
                room.setSchool(school);
                room.setRoomNumber(row.getRoomNumber().trim());
                room.setRoomBuilding(BulkValues.text(row.getRoomBuilding()));
            }
            if (row.getRoomCapacity() != null) room.setRoomCapacity(row.getRoomCapacity());
            if (roomType != null) room.setRoomType(roomType);

            toSave.add(room);
            if (isNew) report.created(); else report.updated();
        }

        roomRepository.saveAll(toSave);
    }

    private static String roomKey(String roomNumber, String roomBuilding) {
        return roomNumber.trim() + "|" + (roomBuilding == null ? "" : roomBuilding.trim());
    }
}
