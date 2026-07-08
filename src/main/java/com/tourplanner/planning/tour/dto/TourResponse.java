package com.tourplanner.planning.tour.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourResponse {

    private UUID tourId;
    private UUID userId;
    private String title;
    private LocalDate startDay;
    private LocalDate endDay;
    private String tourType;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<DayResponse> days;
}
