package com.tourplanner.planning.tour.service;

import com.tourplanner.planning.location.entity.Location;
import com.tourplanner.planning.stop.entity.Stop;
import com.tourplanner.planning.tour.dto.DayRequest;
import com.tourplanner.planning.tour.dto.DayResponse;
import com.tourplanner.planning.tour.entity.Day;
import com.tourplanner.planning.tour.entity.Tour;
import com.tourplanner.planning.tour.repository.DayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class DayServiceImplTest {

    @Mock
    private DayRepository dayRepository;

    @InjectMocks
    private DayServiceImpl dayService;

    private Tour testTour;
    private Day testDay;
    private UUID dayId;
    private UUID tourId;

    @BeforeEach
    void setUp() {
        tourId = UUID.randomUUID();
        dayId = UUID.randomUUID();

        testTour = Tour.builder()
                .tourId(tourId)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build();

        testDay = Day.builder()
                .dayId(dayId)
                .tour(testTour)
                .dayNo(1)
                .date(LocalDate.of(2026, 7, 1))
                .lodgingId(null)
                .stops(new ArrayList<>())
                .build();
    }

    // Verifies that fetching a day by its ID returns the correct day with all mapped fields
    @Test
    void getDayById_existingDay_returnsDayResponse() {
        when(dayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

        DayResponse response = dayService.getDayById(dayId);

        assertThat(response.getDayId()).isEqualTo(dayId);
        assertThat(response.getTourId()).isEqualTo(tourId);
        assertThat(response.getDayNo()).isEqualTo(1);
        assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.getStops()).isEmpty();

        verify(dayRepository).findById(dayId);
    }

    // Verifies that fetching a non-existent day throws a RuntimeException
    @Test
    void getDayById_nonExistingDay_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(dayRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dayService.getDayById(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Day not found with id:");
    }

    // Verifies that all days for a given tour are returned in order by day number
    @Test
    void getDaysByTourId_returnsDayList() {
        Day day2 = Day.builder()
                .dayId(UUID.randomUUID())
                .tour(testTour)
                .dayNo(2)
                .date(LocalDate.of(2026, 7, 2))
                .stops(new ArrayList<>())
                .build();

        when(dayRepository.findByTour_TourIdOrderByDayNo(tourId)).thenReturn(List.of(testDay, day2));

        List<DayResponse> responses = dayService.getDaysByTourId(tourId);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getDayNo()).isEqualTo(1);
        assertThat(responses.get(1).getDayNo()).isEqualTo(2);

        verify(dayRepository).findByTour_TourIdOrderByDayNo(tourId);
    }

    // Verifies that an empty list is returned when a tour has no days
    @Test
    void getDaysByTourId_noDays_returnsEmptyList() {
        when(dayRepository.findByTour_TourIdOrderByDayNo(tourId)).thenReturn(Collections.emptyList());

        List<DayResponse> responses = dayService.getDaysByTourId(tourId);

        assertThat(responses).isEmpty();
    }

    // Verifies that updating a day with a lodging ID correctly sets the lodging
    @Test
    void updateDay_withLodgingId_updatesLodging() {
        UUID lodgingId = UUID.randomUUID();
        DayRequest request = DayRequest.builder()
                .lodgingId(lodgingId)
                .build();

        when(dayRepository.findById(dayId)).thenReturn(Optional.of(testDay));
        when(dayRepository.save(any(Day.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DayResponse response = dayService.updateDay(dayId, request);

        assertThat(response.getLodgingId()).isEqualTo(lodgingId);
        assertThat(response.getDayId()).isEqualTo(dayId);

        verify(dayRepository).save(testDay);
    }

    // Verifies that passing a null lodging ID preserves the existing lodging value
    @Test
    void updateDay_withNullLodgingId_doesNotChangeLodging() {
        UUID existingLodgingId = UUID.randomUUID();
        testDay.setLodgingId(existingLodgingId);

        DayRequest request = DayRequest.builder()
                .lodgingId(null)
                .build();

        when(dayRepository.findById(dayId)).thenReturn(Optional.of(testDay));
        when(dayRepository.save(any(Day.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DayResponse response = dayService.updateDay(dayId, request);

        assertThat(response.getLodgingId()).isEqualTo(existingLodgingId);
    }

    // Verifies that updating a non-existent day throws a RuntimeException
    @Test
    void updateDay_nonExistingDay_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        DayRequest request = DayRequest.builder().lodgingId(UUID.randomUUID()).build();

        when(dayRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dayService.updateDay(nonExistentId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Day not found with id:");
    }

    // Verifies that clearing a day removes all stops and sets lodging to null
    @Test
    void clearDay_clearsStopsAndLodging() {
        UUID locationId = UUID.randomUUID();
        Location location = Location.builder().locationId(locationId).build();

        Stop stop = Stop.builder()
                .stopId(UUID.randomUUID())
                .day(testDay)
                .location(location)
                .stopOrder(1)
                .duration(60)
                .activities(new ArrayList<>())
                .build();

        testDay.setStops(new ArrayList<>(List.of(stop)));
        testDay.setLodgingId(UUID.randomUUID());

        when(dayRepository.findById(dayId)).thenReturn(Optional.of(testDay));
        when(dayRepository.save(any(Day.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DayResponse response = dayService.clearDay(dayId);

        assertThat(response.getLodgingId()).isNull();
        assertThat(response.getStops()).isEmpty();

        verify(dayRepository).save(testDay);
    }

    // Verifies that clearing a non-existent day throws a RuntimeException
    @Test
    void clearDay_nonExistingDay_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(dayRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dayService.clearDay(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Day not found with id:");
    }

    // Verifies that stops are correctly mapped to StopResponse with locationId, stopOrder, and duration
    @Test
    void getDayById_dayWithStops_mapsStopsCorrectly() {
        UUID locationId = UUID.randomUUID();
        Location location = Location.builder().locationId(locationId).build();

        Stop stop = Stop.builder()
                .stopId(UUID.randomUUID())
                .day(testDay)
                .location(location)
                .stopOrder(1)
                .duration(90)
                .activities(new ArrayList<>())
                .build();

        testDay.setStops(List.of(stop));

        when(dayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

        DayResponse response = dayService.getDayById(dayId);

        assertThat(response.getStops()).hasSize(1);
        assertThat(response.getStops().get(0).getLocationId()).isEqualTo(locationId);
        assertThat(response.getStops().get(0).getStopOrder()).isEqualTo(1);
        assertThat(response.getStops().get(0).getDuration()).isEqualTo(90);
    }

    // Verifies that a day with null stops list returns an empty list instead of null
    @Test
    void getDayById_dayWithNullStops_returnsEmptyStopsList() {
        testDay.setStops(null);
        when(dayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

        DayResponse response = dayService.getDayById(dayId);

        assertThat(response.getStops()).isEmpty();
    }

    // Verifies that a stop with no associated location maps locationId as null without errors
    @Test
    void getDayById_stopWithNullLocation_mapsLocationIdAsNull() {
        Stop stop = Stop.builder()
                .stopId(UUID.randomUUID())
                .day(testDay)
                .location(null)
                .stopOrder(1)
                .duration(60)
                .activities(new ArrayList<>())
                .build();

        testDay.setStops(List.of(stop));

        when(dayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

        DayResponse response = dayService.getDayById(dayId);

        assertThat(response.getStops().get(0).getLocationId()).isNull();
    }
}
