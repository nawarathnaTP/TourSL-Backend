package com.tourplanner.planning.tour.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuideTourPackageResponse {

    private UUID packageId;
    private UUID tourId;
    private String tourTitle;
    private String description;
    private String coverImageUrl;
    private Integer maxSlots;
    private Integer availableSlots;
    private BigDecimal pricePerSlot;
    private Boolean isPublished;
    private String status;
}
