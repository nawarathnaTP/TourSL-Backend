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
public class RouteResponse {

    private UUID routeId;
    private UUID startStopId;
    private UUID endStopId;
    private TransportOptionResponse transport;
    private BigDecimal distance;
    private Integer time;
    private BigDecimal cost;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
