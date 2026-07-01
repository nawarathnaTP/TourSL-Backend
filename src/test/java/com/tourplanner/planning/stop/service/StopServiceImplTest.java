package com.tourplanner.planning.stop.service;

import com.tourplanner.planning.location.dto.LocationRequest;
import com.tourplanner.planning.location.entity.Location;
import com.tourplanner.planning.location.service.LocationService;
import com.tourplanner.planning.route.repository.RouteRepository;
import com.tourplanner.planning.stop.dto.StopRequest;
import com.tourplanner.planning.stop.dto.StopResponse;
import com.tourplanner.planning.stop.entity.Activity;
import com.tourplanner.planning.stop.entity.Stop;
import com.tourplanner.planning.stop.repository.StopRepository;
import com.tourplanner.planning.tour.entity.Day;
import com.tourplanner.planning.tour.entity.Tour;
import com.tourplanner.planning.tour.repository.DayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StopServiceImplTest {

    @Mock
    private StopRepository stopRepository;

    @Mock
    private DayRepository dayRepository;

    @Mock
    private LocationService locationService;

    @Mock
    private RouteRepository routeRepository;

    @InjectMocks
    private StopServiceImpl stopService;

    private UUID stopId;
    private UUID dayId;
    private UUID locationId;
    private Day testDay;
    private Location testLocation;
    private Stop testStop;

    @BeforeEach
    void setUp() {
        stopId = UUID.randomUUID();
        dayId = UUID.randomUUID();
        locationId = UUID.randomUUID();

        Tour testTour = Tour.builder()
                .tourId(UUID.randomUUID())
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build();

        testDay = Day.builder()
                .dayId(dayId)
                .tour(testTour)
                .dayNo(1)
                .date(LocalDate.of(2026, 7, 1))
                .stops(new ArrayList<>())
                .build();

        testLocation = Location.builder()
                .locationId(locationId)
                .placeName("Sigiriya")
                .latitude(BigDecimal.valueOf(7.957))
                .longitude(BigDecimal.valueOf(80.760))
                .build();

        testStop = Stop.builder()
                .stopId(stopId)
                .day(testDay)
                .location(testLocation)
                .stopOrder(1)
                .duration(120)
                .activities(new ArrayList<>())
                .build();
    }

    // Verifies that a stop is created with the correct day, location, order, and duration
    @Test
    void addStop_validRequest_returnsStopResponse() {
        LocationRequest locationRequest = LocationRequest.builder()
                .externalId("ext-123")
                .placeName("Sigiriya")
                .latitude(BigDecimal.valueOf(7.957))
                .longitude(BigDecimal.valueOf(80.760))
                .build();

        StopRequest request = StopRequest.builder()
                .dayId(dayId)
                .location(locationRequest)
                .stopOrder(1)
                .duration(120)
                .build();

        when(dayRepository.findById(dayId)).thenReturn(Optional.of(testDay));
        when(locationService.findOrCreate(any(LocationRequest.class))).thenReturn(testLocation);
        when(stopRepository.save(any(Stop.class))).thenAnswer(invocation -> {
            Stop stop = invocation.getArgument(0);
            stop.setStopId(stopId);
            return stop;
        });

        StopResponse response = stopService.addStop(request);

        assertThat(response.getStopId()).isEqualTo(stopId);
        assertThat(response.getDayId()).isEqualTo(dayId);
        assertThat(response.getLocationId()).isEqualTo(locationId);
        assertThat(response.getStopOrder()).isEqualTo(1);
        assertThat(response.getDuration()).isEqualTo(120);

        verify(locationService).findOrCreate(any(LocationRequest.class));
        verify(stopRepository).save(any(Stop.class));
    }

    // Verifies that adding a stop with a non-existent day throws an exception
    @Test
    void addStop_dayNotFound_throwsException() {
        LocationRequest locationRequest = LocationRequest.builder()
                .externalId("ext-123")
                .placeName("Sigiriya")
                .build();

        StopRequest request = StopRequest.builder()
                .dayId(UUID.randomUUID())
                .location(locationRequest)
                .build();

        when(dayRepository.findById(request.getDayId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stopService.addStop(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Day not found with id:");
    }

    // Verifies that fetching a stop by ID returns the correct response with activities
    @Test
    void getStopById_existingStop_returnsStopResponse() {
        when(stopRepository.findById(stopId)).thenReturn(Optional.of(testStop));

        StopResponse response = stopService.getStopById(stopId);

        assertThat(response.getStopId()).isEqualTo(stopId);
        assertThat(response.getDayId()).isEqualTo(dayId);
        assertThat(response.getLocationId()).isEqualTo(locationId);
        assertThat(response.getActivities()).isEmpty();
    }

    // Verifies that fetching a non-existent stop throws a RuntimeException
    @Test
    void getStopById_nonExistingStop_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(stopRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stopService.getStopById(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stop not found with id:");
    }

    // Verifies that activities are correctly mapped in the stop response
    @Test
    void getStopById_withActivities_mapsActivitiesCorrectly() {
        Activity activity = Activity.builder()
                .activityId(UUID.randomUUID())
                .stop(testStop)
                .duration(60)
                .description("Climb the rock")
                .bookingId(UUID.randomUUID())
                .build();
        testStop.setActivities(List.of(activity));

        when(stopRepository.findById(stopId)).thenReturn(Optional.of(testStop));

        StopResponse response = stopService.getStopById(stopId);

        assertThat(response.getActivities()).hasSize(1);
        assertThat(response.getActivities().get(0).getDescription()).isEqualTo("Climb the rock");
        assertThat(response.getActivities().get(0).getDuration()).isEqualTo(60);
    }

    // Verifies that all stops for a day are returned in order
    @Test
    void getStopsByDayId_returnsOrderedStops() {
        Stop stop2 = Stop.builder()
                .stopId(UUID.randomUUID())
                .day(testDay)
                .location(testLocation)
                .stopOrder(2)
                .duration(60)
                .activities(new ArrayList<>())
                .build();

        when(stopRepository.findByDay_DayIdOrderByStopOrder(dayId)).thenReturn(List.of(testStop, stop2));

        List<StopResponse> responses = stopService.getStopsByDayId(dayId);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getStopOrder()).isEqualTo(1);
        assertThat(responses.get(1).getStopOrder()).isEqualTo(2);
    }

    // Verifies that an empty list is returned when a day has no stops
    @Test
    void getStopsByDayId_noStops_returnsEmptyList() {
        when(stopRepository.findByDay_DayIdOrderByStopOrder(dayId)).thenReturn(Collections.emptyList());

        List<StopResponse> responses = stopService.getStopsByDayId(dayId);

        assertThat(responses).isEmpty();
    }

    // Verifies that updating a stop's duration only changes the duration
    @Test
    void updateStop_durationOnly_updatesDuration() {
        StopRequest request = StopRequest.builder()
                .duration(90)
                .build();

        when(stopRepository.findById(stopId)).thenReturn(Optional.of(testStop));
        when(stopRepository.save(any(Stop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StopResponse response = stopService.updateStop(stopId, request);

        assertThat(response.getDuration()).isEqualTo(90);
        assertThat(response.getLocationId()).isEqualTo(locationId);
    }

    // Verifies that updating a stop's location changes the location reference
    @Test
    void updateStop_newLocation_updatesLocation() {
        UUID newLocationId = UUID.randomUUID();
        Location newLocation = Location.builder()
                .locationId(newLocationId)
                .placeName("Kandy")
                .latitude(BigDecimal.valueOf(7.291))
                .longitude(BigDecimal.valueOf(80.636))
                .build();

        LocationRequest locationRequest = LocationRequest.builder()
                .externalId("ext-456")
                .placeName("Kandy")
                .latitude(BigDecimal.valueOf(7.291))
                .longitude(BigDecimal.valueOf(80.636))
                .build();

        StopRequest request = StopRequest.builder()
                .location(locationRequest)
                .build();

        when(stopRepository.findById(stopId)).thenReturn(Optional.of(testStop));
        when(locationService.findOrCreate(any(LocationRequest.class))).thenReturn(newLocation);
        when(stopRepository.save(any(Stop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StopResponse response = stopService.updateStop(stopId, request);

        assertThat(response.getLocationId()).isEqualTo(newLocationId);
    }

    // Verifies that updating a non-existent stop throws an exception
    @Test
    void updateStop_stopNotFound_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        StopRequest request = StopRequest.builder().duration(60).build();

        when(stopRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stopService.updateStop(nonExistentId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stop not found with id:");
    }

    // Verifies that reorderStops assigns new stop orders based on the provided ID list
    @Test
    void reorderStops_validIds_reordersCorrectly() {
        Stop stop2 = Stop.builder()
                .stopId(UUID.randomUUID())
                .day(testDay).location(testLocation).stopOrder(2).duration(60)
                .activities(new ArrayList<>()).build();
        Stop stop3 = Stop.builder()
                .stopId(UUID.randomUUID())
                .day(testDay).location(testLocation).stopOrder(3).duration(45)
                .activities(new ArrayList<>()).build();

        List<Stop> stops = new ArrayList<>(List.of(testStop, stop2, stop3));
        List<UUID> newOrder = List.of(stop3.getStopId(), testStop.getStopId(), stop2.getStopId());

        when(stopRepository.findByDay_DayIdOrderByStopOrder(dayId)).thenReturn(stops);
        when(stopRepository.saveAllAndFlush(anyList())).thenReturn(stops);

        // After reorder, return in the new order
        when(stopRepository.findByDay_DayIdOrderByStopOrder(dayId))
                .thenReturn(stops)
                .thenReturn(List.of(stop3, testStop, stop2));

        List<StopResponse> responses = stopService.reorderStops(dayId, newOrder);

        assertThat(responses).hasSize(3);
        verify(stopRepository, times(2)).saveAllAndFlush(anyList());
    }

    // Verifies that reorderStops throws when the stop ID count doesn't match
    @Test
    void reorderStops_mismatchedCount_throwsException() {
        when(stopRepository.findByDay_DayIdOrderByStopOrder(dayId)).thenReturn(List.of(testStop));

        List<UUID> tooMany = List.of(UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> stopService.reorderStops(dayId, tooMany))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("do not match the number of stops");
    }

    // Verifies that moveStop changes the stop's day and order for a cross-day move
    @Test
    void moveStop_crossDay_changesDay() {
        UUID targetDayId = UUID.randomUUID();
        Day targetDay = Day.builder()
                .dayId(targetDayId)
                .tour(testDay.getTour())
                .dayNo(2)
                .date(LocalDate.of(2026, 7, 2))
                .stops(new ArrayList<>())
                .build();

        when(stopRepository.findById(stopId)).thenReturn(Optional.of(testStop));
        when(dayRepository.findById(targetDayId)).thenReturn(Optional.of(targetDay));
        when(stopRepository.findByDay_DayIdOrderByStopOrder(dayId)).thenReturn(new ArrayList<>(List.of(testStop)));
        when(stopRepository.findByDay_DayIdOrderByStopOrder(targetDayId)).thenReturn(new ArrayList<>());
        when(stopRepository.saveAllAndFlush(anyList())).thenReturn(Collections.emptyList());
        when(stopRepository.saveAndFlush(any(Stop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StopResponse response = stopService.moveStop(stopId, targetDayId, 1);

        assertThat(response.getDayId()).isEqualTo(targetDayId);
        assertThat(response.getStopOrder()).isEqualTo(1);
    }

    // Verifies that moveStop throws when the stop is not found
    @Test
    void moveStop_stopNotFound_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(stopRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stopService.moveStop(nonExistentId, dayId, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stop not found with id:");
    }

    // Verifies that moveStop throws when the target day is not found
    @Test
    void moveStop_targetDayNotFound_throwsException() {
        UUID fakeDayId = UUID.randomUUID();
        when(stopRepository.findById(stopId)).thenReturn(Optional.of(testStop));
        when(dayRepository.findById(fakeDayId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stopService.moveStop(stopId, fakeDayId, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Day not found with id:");
    }

    // Verifies that an existing stop is deleted via the repository
    @Test
    void deleteStop_existingStop_deletesSuccessfully() {
        when(stopRepository.findById(stopId)).thenReturn(Optional.of(testStop));

        stopService.deleteStop(stopId);

        verify(stopRepository).delete(testStop);
    }

    // Verifies that deleting a non-existent stop throws a RuntimeException
    @Test
    void deleteStop_nonExistingStop_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(stopRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stopService.deleteStop(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stop not found with id:");
    }

    // Verifies that a stop with null activities returns an empty list instead of null
    @Test
    void getStopById_nullActivities_returnsEmptyList() {
        testStop.setActivities(null);
        when(stopRepository.findById(stopId)).thenReturn(Optional.of(testStop));

        StopResponse response = stopService.getStopById(stopId);

        assertThat(response.getActivities()).isEmpty();
    }
}
