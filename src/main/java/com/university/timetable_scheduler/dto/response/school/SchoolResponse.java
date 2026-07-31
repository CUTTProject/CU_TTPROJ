package com.university.timetable_scheduler.dto.response.school;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
import com.university.timetable_scheduler.status.SchoolEnum;

@Getter
@Setter
public class SchoolResponse {
    private UUID id;
    private String schoolName;
    private String schoolAddress;
    private String schoolAdminEmail;
    private String schoolPhone;
    private SchoolEnum.SchoolStatus schoolStatus;
}
