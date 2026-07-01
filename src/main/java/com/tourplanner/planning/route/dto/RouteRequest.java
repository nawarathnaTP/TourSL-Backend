package com.tourplanner.planning.route.dto;

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
public class RouteRequest {

    private UUID startStopId;
    private UUID endStopId;
    private String transportType;
    private String transportLabel;
    private BigDecimal distance;
    private Integer time;
    private BigDecimal cost;
}
