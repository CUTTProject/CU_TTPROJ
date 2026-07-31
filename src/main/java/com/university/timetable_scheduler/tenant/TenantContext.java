package com.university.timetable_scheduler.tenant;

import java.util.UUID;

public class TenantContext {
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    public static UUID getSchoolId() { return CURRENT.get(); }
    public static void setSchoolId(UUID id) { CURRENT.set(id); }
    public static void clear() { CURRENT.remove(); }
}

