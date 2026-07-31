package com.university.timetable_scheduler.service.impl;

import com.university.timetable_scheduler.service.UtilityService;

import java.util.UUID;

public class UtilityServiceImpl implements UtilityService {
    public static String generateConflictKey (UUID id1, UUID id2) {
        return (id1.compareTo(id2) <= 0)
                ? id1.toString() + "-" + id2.toString()
                : id2.toString() + "-" + id1.toString();
    }
}
