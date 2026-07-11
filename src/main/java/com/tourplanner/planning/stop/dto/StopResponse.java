package com.tourplanner.planning.stop.dto;

import com.tourplanner.planning.location.dto.LocationResponse;
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
    private LocationResponse location;
    private Integer stopOrder;
    private Integer duration;
    private List<ActivityResponse> activities;
}
