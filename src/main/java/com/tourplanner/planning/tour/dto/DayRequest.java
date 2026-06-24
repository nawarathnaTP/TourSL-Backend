package com.tourplanner.planning.tour.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayRequest {

    private UUID tourId;
    private Integer dayNo;
    private LocalDate date;
    private UUID lodgingId;
}
