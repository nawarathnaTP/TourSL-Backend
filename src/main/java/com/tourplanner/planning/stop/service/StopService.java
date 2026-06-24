package com.tourplanner.planning.stop.service;

import com.tourplanner.planning.stop.dto.StopRequest;
import com.tourplanner.planning.stop.dto.StopResponse;

import java.util.List;
import java.util.UUID;

public interface StopService {

    StopResponse addStop(StopRequest request);

    StopResponse getStopById(UUID stopId);

    List<StopResponse> getStopsByDayId(UUID dayId);

    StopResponse updateStop(UUID stopId, StopRequest request);

    List<StopResponse> reorderStops(UUID dayId, List<UUID> stopIds);

    void deleteStop(UUID stopId);
}
