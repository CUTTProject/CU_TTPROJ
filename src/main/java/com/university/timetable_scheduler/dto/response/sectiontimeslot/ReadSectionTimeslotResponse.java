package com.university.timetable_scheduler.dto.response.sectiontimeslot;

import com.university.timetable_scheduler.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ReadSectionTimeslotResponse extends BaseResponse {
    private Data data;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ReadSectionTimeslotResponseData")
    public static class Data {
        private List<SectionTimeslotResponse> sectionTimeslots;
    }
}
