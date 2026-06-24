package com.tourplanner.planning.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteOptionResponse {

    private UUID optionId;
    private UUID routeId;
    private UUID transportId;
    private String transportType;
    private String transportLabel;
    private BigDecimal distance;
    private Integer time;
    private BigDecimal cost;
    private Boolean isSelected;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime selectedAt;
}
