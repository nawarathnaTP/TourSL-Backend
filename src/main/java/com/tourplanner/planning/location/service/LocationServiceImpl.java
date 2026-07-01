package com.tourplanner.planning.location.service;

import com.tourplanner.planning.location.dto.LocationRequest;
import com.tourplanner.planning.location.dto.LocationResponse;
import com.tourplanner.planning.location.entity.Location;
import com.tourplanner.planning.location.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;

    @Override
    @Transactional(readOnly = true)
    public LocationResponse getLocationById(UUID locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + locationId));
        return mapToResponse(location);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getAllLocations() {
        return locationRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public Location findOrCreate(LocationRequest request) {
        if (request.getExternalId() != null) {
            return locationRepository.findByExternalId(request.getExternalId())
                    .orElseGet(() -> locationRepository.save(Location.builder()
                            .externalId(request.getExternalId())
                            .placeName(request.getPlaceName())
                            .latitude(request.getLatitude())
                            .longitude(request.getLongitude())
                            .imageUrl(request.getImageUrl())
                            .build()));
        }

        return locationRepository.save(Location.builder()
                .placeName(request.getPlaceName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .imageUrl(request.getImageUrl())
                .build());
    }

    private LocationResponse mapToResponse(Location location) {
        return LocationResponse.builder()
                .locationId(location.getLocationId())
                .externalId(location.getExternalId())
                .placeName(location.getPlaceName())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .imageUrl(location.getImageUrl())
                .build();
    }
}
