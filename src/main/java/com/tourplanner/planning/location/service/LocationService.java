package com.tourplanner.planning.location.service;

import com.tourplanner.planning.location.dto.LocationRequest;
import com.tourplanner.planning.location.dto.LocationResponse;

import java.util.List;
import java.util.UUID;

public interface LocationService {

    LocationResponse createLocation(LocationRequest request);

    LocationResponse getLocationById(UUID locationId);

    List<LocationResponse> getAllLocations();

    LocationResponse updateLocation(UUID locationId, LocationRequest request);

    void deleteLocation(UUID locationId);
}
