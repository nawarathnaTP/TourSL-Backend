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
public class TourRequest {

    private UUID userId;
    private LocalDate startDay;
    private LocalDate endDay;
}
