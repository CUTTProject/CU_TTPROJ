package com.university.timetable_scheduler.bulk;

import com.university.timetable_scheduler.status.CourseEnum;
import com.university.timetable_scheduler.status.RoomEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BulkValuesTest {

    private final BulkRow<Object> row = new BulkRow<>(7, new Object());

    @Test
    @DisplayName("enum cells forgive case, spaces, hyphens and a bare level number")
    void parsesEnumsLeniently() {
        BulkUploadReport report = new BulkUploadReport();
        assertThat(BulkValues.parseEnum(RoomEnum.RoomType.class, " lecture theatre ", row, "roomType", report))
                .isEqualTo(RoomEnum.RoomType.LECTURE_THEATRE);
        assertThat(BulkValues.parseEnum(RoomEnum.RoomType.class, "seminar-room", row, "roomType", report))
                .isEqualTo(RoomEnum.RoomType.SEMINAR_ROOM);
        assertThat(BulkValues.parseEnum(CourseEnum.CourseLevel.class, "200", row, "courseLevel", report))
                .isEqualTo(CourseEnum.CourseLevel.LEVEL_200);
        assertThat(BulkValues.parseEnum(CourseEnum.CourseLevel.class, "", row, "courseLevel", report)).isNull();
        assertThat(report.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("an unrecognised enum value rejects the row and lists the allowed values")
    void rejectsUnknownEnum() {
        BulkUploadReport report = new BulkUploadReport();
        assertThat(BulkValues.parseEnum(RoomEnum.RoomType.class, "gym", row, "roomType", report)).isNull();
        assertThat(report.isRejected(row)).isTrue();
        assertThat(report.getErrors().get(0).getMessage()).contains("LECTURE_THEATRE");
    }

    @Test
    void parsesBooleans() {
        BulkUploadReport report = new BulkUploadReport();
        assertThat(BulkValues.parseBoolean("Yes", row, "isCore", report)).isTrue();
        assertThat(BulkValues.parseBoolean("0", row, "isCore", report)).isFalse();
        assertThat(BulkValues.parseBoolean(" ", row, "isCore", report)).isNull();
        assertThat(report.getErrors()).isEmpty();
        assertThat(BulkValues.parseBoolean("maybe", row, "isCore", report)).isNull();
        assertThat(report.isRejected(row)).isTrue();
    }

    @Test
    void splitsSlashLists() {
        assertThat(BulkValues.splitList(" LT1 / LT2//LT1 ")).containsExactly("LT1", "LT2");
        assertThat(BulkValues.splitList(null)).isEmpty();
    }
}
