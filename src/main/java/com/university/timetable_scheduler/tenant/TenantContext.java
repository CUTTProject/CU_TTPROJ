package com.university.timetable_scheduler.tenant;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * The current request's school, held per thread. Populated by {@code JwtAuthFilter} and cleared in
 * its {@code finally}.
 *
 * <p><b>Must stay a plain {@link ThreadLocal}.</b> {@code InheritableThreadLocal} looks like the way
 * to reach a background thread and is a cross-tenant leak: a pooled thread inherits one school's id
 * from whichever request created it and keeps it for the application's life. Use {@link #runWith}.
 */
public class TenantContext {
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    public static UUID getSchoolId() { return CURRENT.get(); }
    public static void setSchoolId(UUID id) { CURRENT.set(id); }
    public static void clear() { CURRENT.remove(); }

    /**
     * Runs {@code body} with the tenant bound, restoring what was bound before — so this is safe on
     * a request thread too, and {@code remove()} stops a pooled thread retaining an entry.
     */
    public static void runWith(UUID schoolId, Runnable body) {
        UUID previous = CURRENT.get();
        CURRENT.set(schoolId);
        try {
            body.run();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    /** {@link #runWith} for work that returns a value. */
    public static <T> T callWith(UUID schoolId, Supplier<T> body) {
        UUID previous = CURRENT.get();
        CURRENT.set(schoolId);
        try {
            return body.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
