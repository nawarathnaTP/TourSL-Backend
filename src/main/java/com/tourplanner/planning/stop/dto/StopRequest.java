package com.tourplanner.planning.stop.dto;

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
    private UUID locationId;
    private Integer stopOrder;
    private Integer duration;
}
