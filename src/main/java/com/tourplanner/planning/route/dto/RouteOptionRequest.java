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
public class RouteOptionRequest {

    private UUID routeId;
    private UUID transportId;
    private BigDecimal distance;
    private Integer time;
    private BigDecimal cost;
}
