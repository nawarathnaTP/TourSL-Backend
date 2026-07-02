package com.tourplanner.planning.location.service;

import com.tourplanner.planning.location.dto.LocationRequest;
import com.tourplanner.planning.location.dto.LocationResponse;
import com.tourplanner.planning.location.entity.Location;
import com.tourplanner.planning.location.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private LocationServiceImpl locationService;

    private UUID locationId;
    private Location testLocation;

    @BeforeEach
    void setUp() {
        locationId = UUID.randomUUID();

        testLocation = Location.builder()
                .locationId(locationId)
                .externalId("ext-123")
                .placeName("Sigiriya")
                .latitude(BigDecimal.valueOf(7.957))
                .longitude(BigDecimal.valueOf(80.760))
                .imageUrl("https://example.com/sigiriya.jpg")
                .build();
    }

    // Verifies that fetching a location by ID returns the correct response
    @Test
    void getLocationById_existingLocation_returnsLocationResponse() {
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(testLocation));

        LocationResponse response = locationService.getLocationById(locationId);

        assertThat(response.getLocationId()).isEqualTo(locationId);
        assertThat(response.getExternalId()).isEqualTo("ext-123");
        assertThat(response.getPlaceName()).isEqualTo("Sigiriya");
        assertThat(response.getLatitude()).isEqualByComparingTo(BigDecimal.valueOf(7.957));
        assertThat(response.getLongitude()).isEqualByComparingTo(BigDecimal.valueOf(80.760));
        assertThat(response.getImageUrl()).isEqualTo("https://example.com/sigiriya.jpg");
    }

    // Verifies that fetching a non-existent location throws a RuntimeException
    @Test
    void getLocationById_nonExistingLocation_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(locationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.getLocationById(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Location not found with id:");
    }

    // Verifies that getAllLocations returns all locations mapped to responses
    @Test
    void getAllLocations_returnsAllLocations() {
        Location location2 = Location.builder()
                .locationId(UUID.randomUUID())
                .placeName("Kandy")
                .latitude(BigDecimal.valueOf(7.291))
                .longitude(BigDecimal.valueOf(80.636))
                .build();

        when(locationRepository.findAll()).thenReturn(List.of(testLocation, location2));

        List<LocationResponse> responses = locationService.getAllLocations();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getPlaceName()).isEqualTo("Sigiriya");
        assertThat(responses.get(1).getPlaceName()).isEqualTo("Kandy");
    }

    // Verifies that getAllLocations returns an empty list when no locations exist
    @Test
    void getAllLocations_noLocations_returnsEmptyList() {
        when(locationRepository.findAll()).thenReturn(Collections.emptyList());

        List<LocationResponse> responses = locationService.getAllLocations();

        assertThat(responses).isEmpty();
    }

    // Verifies that findOrCreate returns an existing location when externalId matches
    @Test
    void findOrCreate_existingExternalId_returnsExistingLocation() {
        LocationRequest request = LocationRequest.builder()
                .externalId("ext-123")
                .placeName("Sigiriya")
                .latitude(BigDecimal.valueOf(7.957))
                .longitude(BigDecimal.valueOf(80.760))
                .build();

        when(locationRepository.findByExternalId("ext-123")).thenReturn(Optional.of(testLocation));

        Location result = locationService.findOrCreate(request);

        assertThat(result.getLocationId()).isEqualTo(locationId);
        assertThat(result.getExternalId()).isEqualTo("ext-123");
        verify(locationRepository, never()).save(any(Location.class));
    }

    // Verifies that findOrCreate creates a new location when externalId does not exist
    @Test
    void findOrCreate_newExternalId_createsNewLocation() {
        LocationRequest request = LocationRequest.builder()
                .externalId("ext-456")
                .placeName("Kandy")
                .latitude(BigDecimal.valueOf(7.291))
                .longitude(BigDecimal.valueOf(80.636))
                .imageUrl("https://example.com/kandy.jpg")
                .build();

        Location newLocation = Location.builder()
                .locationId(UUID.randomUUID())
                .externalId("ext-456")
                .placeName("Kandy")
                .latitude(BigDecimal.valueOf(7.291))
                .longitude(BigDecimal.valueOf(80.636))
                .imageUrl("https://example.com/kandy.jpg")
                .build();

        when(locationRepository.findByExternalId("ext-456")).thenReturn(Optional.empty());
        when(locationRepository.save(any(Location.class))).thenReturn(newLocation);

        Location result = locationService.findOrCreate(request);

        assertThat(result.getPlaceName()).isEqualTo("Kandy");
        assertThat(result.getExternalId()).isEqualTo("ext-456");
        verify(locationRepository).save(any(Location.class));
    }

    // Verifies that findOrCreate creates a new location when externalId is null
    @Test
    void findOrCreate_nullExternalId_createsNewLocation() {
        LocationRequest request = LocationRequest.builder()
                .placeName("Custom Spot")
                .latitude(BigDecimal.valueOf(6.927))
                .longitude(BigDecimal.valueOf(79.861))
                .build();

        Location newLocation = Location.builder()
                .locationId(UUID.randomUUID())
                .placeName("Custom Spot")
                .latitude(BigDecimal.valueOf(6.927))
                .longitude(BigDecimal.valueOf(79.861))
                .build();

        when(locationRepository.save(any(Location.class))).thenReturn(newLocation);

        Location result = locationService.findOrCreate(request);

        assertThat(result.getPlaceName()).isEqualTo("Custom Spot");
        assertThat(result.getExternalId()).isNull();
        verify(locationRepository, never()).findByExternalId(any());
        verify(locationRepository).save(any(Location.class));
    }

    // Verifies that a location with null imageUrl is mapped correctly
    @Test
    void getLocationById_nullImageUrl_returnsNullImageUrl() {
        testLocation.setImageUrl(null);
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(testLocation));

        LocationResponse response = locationService.getLocationById(locationId);

        assertThat(response.getImageUrl()).isNull();
    }

    // Verifies that a location with null externalId is mapped correctly
    @Test
    void getLocationById_nullExternalId_returnsNullExternalId() {
        testLocation.setExternalId(null);
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(testLocation));

        LocationResponse response = locationService.getLocationById(locationId);

        assertThat(response.getExternalId()).isNull();
    }
}
