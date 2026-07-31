package com.university.timetable_scheduler.dto.response.sectiontimeslot;

import com.university.timetable_scheduler.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CreateSectionTimeslotResponse extends BaseResponse {
    private Data data;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "CreateSectionTimeslotResponseData")
    public static class Data {
        private SectionTimeslotResponse sectionTimeslot;
    }
}
