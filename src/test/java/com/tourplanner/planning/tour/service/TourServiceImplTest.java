package com.tourplanner.planning.tour.service;

import com.tourplanner.planning.auth.entity.User;
import com.tourplanner.planning.auth.repository.UserRepository;
import com.tourplanner.planning.tour.dto.TourRequest;
import com.tourplanner.planning.tour.dto.TourResponse;
import com.tourplanner.planning.tour.entity.Tour;
import com.tourplanner.planning.tour.entity.Day;
import com.tourplanner.planning.tour.repository.TourRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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
class TourServiceImplTest {

    @Mock
    private TourRepository tourRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TourServiceImpl tourService;

    private User testUser;
    private Tour testTour;
    private UUID tourId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tourId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();

        testTour = Tour.builder()
                .tourId(tourId)
                .user(testUser)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .days(new ArrayList<>())
                .build();

        // Set up SecurityContext for authenticated user
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("john@example.com", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Verifies that a tour is created with correct fields and the expected number of days
    @Test
    void createTour_validRequest_returnsTourResponse() {
        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> {
            Tour tour = invocation.getArgument(0);
            tour.setTourId(tourId);
            tour.setCreatedAt(OffsetDateTime.now());
            tour.setUpdatedAt(OffsetDateTime.now());
            return tour;
        });

        TourResponse response = tourService.createTour(request);

        assertThat(response.getTourId()).isEqualTo(tourId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getStartDay()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.getEndDay()).isEqualTo(LocalDate.of(2026, 7, 3));
        assertThat(response.getDays()).hasSize(3);

        verify(tourRepository).save(any(Tour.class));
    }

    // Verifies that a tour with the same start and end date creates exactly one day entry
    @Test
    void createTour_singleDay_createsOneDayEntry() {
        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 1))
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> {
            Tour tour = invocation.getArgument(0);
            tour.setTourId(tourId);
            tour.setCreatedAt(OffsetDateTime.now());
            tour.setUpdatedAt(OffsetDateTime.now());
            return tour;
        });

        TourResponse response = tourService.createTour(request);

        assertThat(response.getDays()).hasSize(1);
        assertThat(response.getDays().get(0).getDayNo()).isEqualTo(1);
        assertThat(response.getDays().get(0).getDate()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    // Verifies that creating a tour throws an exception when the authenticated user is not found in the database
    @Test
    void createTour_userNotFound_throwsException() {
        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tourService.createTour(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Authenticated user not found");
    }

    // Verifies that fetching a tour by its ID returns the correct tour with its associated days
    @Test
    void getTourById_existingTour_returnsTourResponse() {
        testTour.setDays(List.of(
                Day.builder().dayId(UUID.randomUUID()).tour(testTour).dayNo(1)
                        .date(LocalDate.of(2026, 7, 1)).stops(new ArrayList<>()).build(),
                Day.builder().dayId(UUID.randomUUID()).tour(testTour).dayNo(2)
                        .date(LocalDate.of(2026, 7, 2)).stops(new ArrayList<>()).build()
        ));

        when(tourRepository.findById(tourId)).thenReturn(Optional.of(testTour));

        TourResponse response = tourService.getTourById(tourId);

        assertThat(response.getTourId()).isEqualTo(tourId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getDays()).hasSize(2);

        verify(tourRepository).findById(tourId);
    }

    // Verifies that fetching a non-existent tour throws a RuntimeException
    @Test
    void getTourById_nonExistingTour_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(tourRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tourService.getTourById(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tour not found with id:");
    }

    // Verifies that all tours belonging to the authenticated user are returned
    @Test
    void getToursByUserId_returnsTourList() {
        Tour tour2 = Tour.builder()
                .tourId(UUID.randomUUID())
                .user(testUser)
                .startDay(LocalDate.of(2026, 8, 1))
                .endDay(LocalDate.of(2026, 8, 5))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .days(new ArrayList<>())
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(tourRepository.findByUser_Id(userId)).thenReturn(List.of(testTour, tour2));

        List<TourResponse> responses = tourService.getToursByUserId(userId);

        assertThat(responses).hasSize(2);
        verify(tourRepository).findByUser_Id(userId);
    }

    // Verifies that an empty list is returned when the user has no tours
    @Test
    void getToursByUserId_noTours_returnsEmptyList() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(tourRepository.findByUser_Id(userId)).thenReturn(Collections.emptyList());

        List<TourResponse> responses = tourService.getToursByUserId(userId);

        assertThat(responses).isEmpty();
    }

    // Verifies that updating a tour with new dates clears old days and regenerates new ones
    @Test
    void updateTour_datesChanged_regeneratesDays() {
        testTour.setDays(new ArrayList<>(List.of(
                Day.builder().dayId(UUID.randomUUID()).tour(testTour).dayNo(1)
                        .date(LocalDate.of(2026, 7, 1)).stops(new ArrayList<>()).build()
        )));

        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 10))
                .endDay(LocalDate.of(2026, 7, 12))
                .build();

        when(tourRepository.findById(tourId)).thenReturn(Optional.of(testTour));
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TourResponse response = tourService.updateTour(tourId, request);

        assertThat(response.getStartDay()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(response.getEndDay()).isEqualTo(LocalDate.of(2026, 7, 12));
        assertThat(response.getDays()).hasSize(3);

        verify(tourRepository).save(any(Tour.class));
    }

    // Verifies that updating a tour with the same dates preserves the existing days unchanged
    @Test
    void updateTour_sameDates_doesNotRegenerateDays() {
        Day existingDay = Day.builder()
                .dayId(UUID.randomUUID()).tour(testTour).dayNo(1)
                .date(LocalDate.of(2026, 7, 1)).stops(new ArrayList<>()).build();
        testTour.setDays(new ArrayList<>(List.of(existingDay)));
        testTour.setStartDay(LocalDate.of(2026, 7, 1));
        testTour.setEndDay(LocalDate.of(2026, 7, 1));

        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 1))
                .build();

        when(tourRepository.findById(tourId)).thenReturn(Optional.of(testTour));
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TourResponse response = tourService.updateTour(tourId, request);

        assertThat(response.getDays()).hasSize(1);
        assertThat(response.getDays().get(0).getDayId()).isEqualTo(existingDay.getDayId());
    }

    // Verifies that updating a non-existent tour throws a RuntimeException
    @Test
    void updateTour_nonExistingTour_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build();

        when(tourRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tourService.updateTour(nonExistentId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tour not found with id:");
    }

    // Verifies that an existing tour is deleted via the repository
    @Test
    void deleteTour_existingTour_deletesSuccessfully() {
        when(tourRepository.findById(tourId)).thenReturn(Optional.of(testTour));

        tourService.deleteTour(tourId);

        verify(tourRepository).delete(testTour);
    }

    // Verifies that deleting a non-existent tour throws a RuntimeException
    @Test
    void deleteTour_nonExistingTour_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(tourRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tourService.deleteTour(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tour not found with id:");
    }

    // Verifies that each generated day has the correct sequential day number and corresponding date
    @Test
    void createTour_daysHaveCorrectDayNumbers() {
        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 5))
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> {
            Tour tour = invocation.getArgument(0);
            tour.setTourId(tourId);
            tour.setCreatedAt(OffsetDateTime.now());
            tour.setUpdatedAt(OffsetDateTime.now());
            return tour;
        });

        TourResponse response = tourService.createTour(request);

        assertThat(response.getDays()).hasSize(5);
        for (int i = 0; i < 5; i++) {
            assertThat(response.getDays().get(i).getDayNo()).isEqualTo(i + 1);
            assertThat(response.getDays().get(i).getDate()).isEqualTo(LocalDate.of(2026, 7, 1).plusDays(i));
        }
    }

    // Verifies that a tour with null days list returns an empty list instead of null
    @Test
    void getTourById_tourWithNullDays_returnsEmptyDaysList() {
        testTour.setDays(null);
        when(tourRepository.findById(tourId)).thenReturn(Optional.of(testTour));

        TourResponse response = tourService.getTourById(tourId);

        assertThat(response.getDays()).isEmpty();
    }
}
