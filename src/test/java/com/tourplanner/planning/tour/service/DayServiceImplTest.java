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
import org.mockito.ArgumentCaptor;
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

    // ==================== getDayById ====================

    @Test
    void getDayById_existingDay_returnsDayResponse() {
        when(dayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

        DayResponse response = dayService.getDayById(dayId);

        assertThat(response.getDayId()).isEqualTo(dayId);
        assertThat(response.getTourId()).isEqualTo(tourId);
        assertThat(response.getDayNo()).isEqualTo(1);
        assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.getStops()).isEmpty();
    }

    @Test
    void getDayById_dayWithLodging_returnsLodgingId() {
        UUID lodgingId = UUID.randomUUID();
        testDay.setLodgingId(lodgingId);

        when(dayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

        DayResponse response = dayService.getDayById(dayId);

        assertThat(response.getLodgingId()).isEqualTo(lodgingId);
    }

    @Test
    void getDayById_dayWithStops_mapsStopsCorrectly() {
        UUID locationId = UUID.randomUUID();
        Location location = Location.builder()
                .locationId(locationId)
                .placeName("Sigiriya")
                .latitude(BigDecimal.valueOf(7.957))
                .longitude(BigDecimal.valueOf(80.760))
                .build();

        Stop stop = Stop.builder()
                .stopId(UUID.randomUUID())
                .day(testDay)
                .location(location)
                .stopOrder(1)
                .duration(120)
                .activities(new ArrayList<>())
                .build();

        testDay.getStops().add(stop);

        when(dayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

        DayResponse response = dayService.getDayById(dayId);

        assertThat(response.getStops()).hasSize(1);
        assertThat(response.getStops().get(0).getLocationId()).isEqualTo(locationId);
        assertThat(response.getStops().get(0).getStopOrder()).isEqualTo(1);
        assertThat(response.getStops().get(0).getDuration()).isEqualTo(120);
        assertThat(response.getStops().get(0).getDayId()).isEqualTo(dayId);
    }

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

        testDay.getStops().add(stop);

        when(dayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

        DayResponse response = dayService.getDayById(dayId);

        assertThat(response.getStops().get(0).getLocationId()).isNull();
    }

    @Test
    void getDayById_nonExistingDay_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(dayRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dayService.getDayById(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Day not found with id: " + nonExistentId);
    }

    // ==================== getDaysByTourId ====================

    @Test
    void getDaysByTourId_tourHasDays_returnsList() {
        Day day2 = Day.builder()
                .dayId(UUID.randomUUID())
                .tour(testTour)
                .dayNo(2)
                .date(LocalDate.of(2026, 7, 2))
                .stops(new ArrayList<>())
                .build();

        Day day3 = Day.builder()
                .dayId(UUID.randomUUID())
                .tour(testTour)
                .dayNo(3)
                .date(LocalDate.of(2026, 7, 3))
                .stops(new ArrayList<>())
                .build();

        when(dayRepository.findByTour_TourIdOrderByDayNo(tourId))
                .thenReturn(List.of(testDay, day2, day3));

        List<DayResponse> responses = dayService.getDaysByTourId(tourId);

        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).getDayNo()).isEqualTo(1);
        assertThat(responses.get(1).getDayNo()).isEqualTo(2);
        assertThat(responses.get(2).getDayNo()).isEqualTo(3);
    }

    @Test
    void getDaysByTourId_tourHasNoDays_returnsEmptyList() {
        when(dayRepository.findByTour_TourIdOrderByDayNo(tourId)).thenReturn(Collections.emptyList());

        List<DayResponse> responses = dayService.getDaysByTourId(tourId);

        assertThat(responses).isEmpty();
    }

    @Test
    void getDaysByTourId_nonExistingTour_returnsEmptyList() {
        UUID nonExistentTourId = UUID.randomUUID();
        when(dayRepository.findByTour_TourIdOrderByDayNo(nonExistentTourId)).thenReturn(Collections.emptyList());

        List<DayResponse> responses = dayService.getDaysByTourId(nonExistentTourId);

        assertThat(responses).isEmpty();
    }

    // ==================== updateDay ====================

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
        verify(dayRepository).save(testDay);
    }

    @Test
    void updateDay_withNullLodgingId_doesNotOverwrite() {
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

    @Test
    void updateDay_nonExistingDay_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        DayRequest request = DayRequest.builder().build();

        when(dayRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dayService.updateDay(nonExistentId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Day not found with id: " + nonExistentId);

        verify(dayRepository, never()).save(any());
    }

    @Test
    void updateDay_preservesOtherFields() {
        UUID lodgingId = UUID.randomUUID();
        DayRequest request = DayRequest.builder()
                .lodgingId(lodgingId)
                .build();

        when(dayRepository.findById(dayId)).thenReturn(Optional.of(testDay));
        when(dayRepository.save(any(Day.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DayResponse response = dayService.updateDay(dayId, request);

        assertThat(response.getDayNo()).isEqualTo(1);
        assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.getTourId()).isEqualTo(tourId);
    }

    // ==================== clearDay ====================

    @Test
    void clearDay_clearsStopsAndLodging() {
        UUID lodgingId = UUID.randomUUID();
        testDay.setLodgingId(lodgingId);

        Stop stop = Stop.builder()
                .stopId(UUID.randomUUID())
                .day(testDay)
                .stopOrder(1)
                .duration(60)
                .activities(new ArrayList<>())
                .build();
        testDay.getStops().add(stop);

        when(dayRepository.findById(dayId)).thenReturn(Optional.of(testDay));
        when(dayRepository.save(any(Day.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DayResponse response = dayService.clearDay(dayId);

        assertThat(response.getLodgingId()).isNull();
        assertThat(response.getStops()).isEmpty();
    }

    @Test
    void clearDay_alreadyEmpty_returnsSuccessfully() {
        when(dayRepository.findById(dayId)).thenReturn(Optional.of(testDay));
        when(dayRepository.save(any(Day.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DayResponse response = dayService.clearDay(dayId);

        assertThat(response.getLodgingId()).isNull();
        assertThat(response.getStops()).isEmpty();
        verify(dayRepository).save(testDay);
    }

    @Test
    void clearDay_preservesDayMetadata() {
        when(dayRepository.findById(dayId)).thenReturn(Optional.of(testDay));
        when(dayRepository.save(any(Day.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DayResponse response = dayService.clearDay(dayId);

        assertThat(response.getDayId()).isEqualTo(dayId);
        assertThat(response.getTourId()).isEqualTo(tourId);
        assertThat(response.getDayNo()).isEqualTo(1);
        assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    void clearDay_nonExistingDay_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(dayRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dayService.clearDay(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Day not found with id: " + nonExistentId);

        verify(dayRepository, never()).save(any());
    }
}
