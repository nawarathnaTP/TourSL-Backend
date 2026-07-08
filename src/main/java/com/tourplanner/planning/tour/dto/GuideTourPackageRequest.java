package com.tourplanner.planning.tour.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuideTourPackageRequest {

    private String description;
    private String coverImageUrl;
    private Integer maxSlots;
    private BigDecimal pricePerSlot;
}
