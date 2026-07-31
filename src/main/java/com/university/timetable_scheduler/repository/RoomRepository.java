package com.university.timetable_scheduler.repository;

import com.university.timetable_scheduler.entity.Room;
import com.university.timetable_scheduler.status.RoomEnum;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoomRepository extends TenantAwareRepository<Room> {

    @Query("""
        SELECT r FROM Room r
        WHERE r.school.id = :schoolId
          AND (r.isDeleted IS NULL OR r.isDeleted = false)
          AND (:id IS NULL OR r.id = :id)
          AND (:roomBuilding IS NULL OR r.roomBuilding = :roomBuilding)
          AND (:roomNumber IS NULL OR r.roomNumber = :roomNumber)
          AND (:roomCapacity IS NULL OR r.roomCapacity = :roomCapacity)
          AND (:roomType IS NULL OR r.roomType = :roomType)
          AND (:roomStatus IS NULL OR r.roomStatus = :roomStatus)
    """)
    List<Room> findRoomByFilter(
            @Param("schoolId") UUID schoolId,
            @Param("id") UUID id,
            @Param("roomBuilding") String roomBuilding,
            @Param("roomNumber") String roomNumber,
            @Param("roomCapacity") Integer roomCapacity,
            @Param("roomType") RoomEnum.RoomType roomType,
            @Param("roomStatus") RoomEnum.RoomStatus roomStatus
    );
}
