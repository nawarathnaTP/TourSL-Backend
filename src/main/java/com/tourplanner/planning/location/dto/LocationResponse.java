package com.tourplanner.planning.location.dto;

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
public class LocationResponse {

    private UUID locationId;
    private String externalId;
    private String placeName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String imageUrl;
}
