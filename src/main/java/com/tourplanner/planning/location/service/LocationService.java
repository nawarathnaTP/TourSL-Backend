package com.tourplanner.planning.location.service;

import com.tourplanner.planning.location.dto.LocationRequest;
import com.tourplanner.planning.location.dto.LocationResponse;
import com.tourplanner.planning.location.entity.Location;

import java.util.List;
import java.util.UUID;

public interface LocationService {

    LocationResponse getLocationById(UUID locationId);

    List<LocationResponse> getAllLocations();

    Location findOrCreate(LocationRequest request);
}
