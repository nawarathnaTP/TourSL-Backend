package com.tourplanner.planning.stop.dto;

import com.tourplanner.planning.location.dto.LocationRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StopRequest {

    private UUID dayId;
    private LocationRequest location;
    private Integer stopOrder;
    private Integer duration;
}
