package com.university.timetable_scheduler.bulk;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An optional row field a school can erase. A missing CSV column, or a JSON property omitted or
 * null, reads as {@code null} and keeps the stored value; an explicitly blank cell, or a JSON
 * {@code ""}, reads as {@code ""} and clears it. Apply with {@link BulkValues#applyClearable}.
 *
 * <p>Other fields get no such distinction: their blank cells read as {@code null}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Clearable {
}
