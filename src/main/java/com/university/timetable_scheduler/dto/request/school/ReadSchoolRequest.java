package com.university.timetable_scheduler.dto.request.school;

import com.university.timetable_scheduler.status.SchoolEnum;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ReadSchoolRequest {
    private UUID id;
    private String schoolName;
    private SchoolEnum.SchoolStatus schoolStatus;
}

