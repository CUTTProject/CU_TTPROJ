package com.university.timetable_scheduler.service.impl;

import com.opencsv.bean.CsvToBeanBuilder;
import com.university.timetable_scheduler.dto.request.room.*;
import com.university.timetable_scheduler.dto.response.room.*;
import com.university.timetable_scheduler.entity.Room;
import com.university.timetable_scheduler.entity.School;
import com.university.timetable_scheduler.mapper.RoomMapper;
import com.university.timetable_scheduler.repository.RoomRepository;
import com.university.timetable_scheduler.repository.SchoolRepository;
import com.university.timetable_scheduler.service.RoomService;
import com.university.timetable_scheduler.status.RoomEnum;
import com.university.timetable_scheduler.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RoomServiceImpl implements RoomService {
    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;
    private final SchoolRepository schoolRepository;

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
    public BulkUploadRoomResponse bulkUploadRooms(MultipartFile file) {
        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            List<BulkUploadRoomFileRequest> rows =
                    new CsvToBeanBuilder<BulkUploadRoomFileRequest>(reader)
                            .withType(BulkUploadRoomFileRequest.class)
                            .withIgnoreLeadingWhiteSpace(true)
                            .build()
                            .parse();

            processRoomRows(rows.stream().map(r -> {
                BulkUploadRoomArrayRequest.Row row = new BulkUploadRoomArrayRequest.Row();
                row.setRoomBuilding(r.getRoomBuilding());
                row.setRoomNumber(r.getRoomNumber());
                row.setRoomCapacity(r.getRoomCapacity());
                row.setRoomType(r.getRoomType());
                return row;
            }).toList(), currentSchool());

            BulkUploadRoomResponse response = new BulkUploadRoomResponse();
            response.setError(false);
            response.setResponseCode("200");
            response.setResponseMessage("Upload complete. " + rows.size() + " room(s) processed.");
            return response;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Room bulk upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public BulkUploadRoomResponse bulkUploadRoomsArray(BulkUploadRoomArrayRequest request) {
        processRoomRows(request.getRooms(), currentSchool());

        BulkUploadRoomResponse response = new BulkUploadRoomResponse();
        response.setError(false);
        response.setResponseCode("200");
        response.setResponseMessage("Upload complete. " + request.getRooms().size() + " room(s) processed.");
        return response;
    }

    private void processRoomRows(List<BulkUploadRoomArrayRequest.Row> rows, School school) {
        UUID schoolId = school.getId();
        Map<String, Room> cache = new HashMap<>();
        roomRepository.findAllBySchool_Id(schoolId)
                .forEach(r -> { if (r.getRoomNumber() != null) cache.put(r.getRoomNumber() + "|" + r.getRoomBuilding(), r); });

        List<Room> toSave = new ArrayList<>();

        for (BulkUploadRoomArrayRequest.Row row : rows) {
            if (row.getRoomNumber() == null || row.getRoomNumber().isBlank()) continue;

            String cacheKey = row.getRoomNumber() + "|" + row.getRoomBuilding();
            Room room = cache.getOrDefault(cacheKey, new Room());
            room.setSchool(school);
            room.setRoomNumber(row.getRoomNumber());
            room.setRoomBuilding(row.getRoomBuilding());

            if (row.getRoomCapacity() != null) room.setRoomCapacity(row.getRoomCapacity());
            if (row.getRoomType() != null && !row.getRoomType().isBlank()) {
                try {
                    room.setRoomType(RoomEnum.RoomType.valueOf(row.getRoomType().trim().toUpperCase()));
                } catch (IllegalArgumentException ignore) {}
            }

            toSave.add(room);
            cache.put(cacheKey, room);
        }

        roomRepository.saveAll(toSave);
    }
}
