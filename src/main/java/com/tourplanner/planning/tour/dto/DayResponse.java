package com.tourplanner.planning.tour.dto;

import com.tourplanner.planning.stop.dto.StopResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayResponse {

    private UUID dayId;
    private UUID tourId;
    private Integer dayNo;
    private LocalDate date;
    private UUID lodgingId;
    private List<StopResponse> stops;
}
