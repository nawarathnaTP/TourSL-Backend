package com.tourplanner.planning.stop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StopResponse {

    private UUID stopId;
    private UUID dayId;
    private UUID locationId;
    private Integer stopOrder;
    private Integer duration;
    private List<ActivityResponse> activities;
}
