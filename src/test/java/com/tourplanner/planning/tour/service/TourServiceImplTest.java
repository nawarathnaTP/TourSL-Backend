package com.tourplanner.planning.tour.service;

import com.tourplanner.planning.auth.entity.User;
import com.tourplanner.planning.auth.repository.UserRepository;
import com.tourplanner.planning.tour.dto.TourRequest;
import com.tourplanner.planning.tour.dto.TourResponse;
import com.tourplanner.planning.tour.entity.Day;
import com.tourplanner.planning.tour.entity.Tour;
import com.tourplanner.planning.tour.repository.TourRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
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

        // Add days to the test tour
        for (int i = 0; i < 3; i++) {
            Day day = Day.builder()
                    .dayId(UUID.randomUUID())
                    .tour(testTour)
                    .dayNo(i + 1)
                    .date(LocalDate.of(2026, 7, 1).plusDays(i))
                    .stops(new ArrayList<>())
                    .build();
            testTour.getDays().add(day);
        }
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setUpSecurityContext(String email) {
        var auth = new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    // ==================== createTour ====================

    @Test
    void createTour_validRequest_createsTourWithDays() {
        setUpSecurityContext("john@example.com");

        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(tourRepository.save(any(Tour.class))).thenReturn(testTour);

        TourResponse response = tourService.createTour(request);

        assertThat(response.getTourId()).isEqualTo(tourId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getStartDay()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.getEndDay()).isEqualTo(LocalDate.of(2026, 7, 3));
        assertThat(response.getDays()).hasSize(3);

        verify(tourRepository).save(any(Tour.class));
    }

    @Test
    void createTour_validRequest_generatesCorrectNumberOfDays() {
        setUpSecurityContext("john@example.com");

        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 5))
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tourService.createTour(request);

        ArgumentCaptor<Tour> tourCaptor = ArgumentCaptor.forClass(Tour.class);
        verify(tourRepository).save(tourCaptor.capture());

        Tour savedTour = tourCaptor.getValue();
        assertThat(savedTour.getDays()).hasSize(5);
    }

    @Test
    void createTour_validRequest_daysHaveCorrectDatesAndNumbers() {
        setUpSecurityContext("john@example.com");

        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 10))
                .endDay(LocalDate.of(2026, 7, 12))
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tourService.createTour(request);

        ArgumentCaptor<Tour> tourCaptor = ArgumentCaptor.forClass(Tour.class);
        verify(tourRepository).save(tourCaptor.capture());

        List<Day> days = tourCaptor.getValue().getDays();
        assertThat(days.get(0).getDayNo()).isEqualTo(1);
        assertThat(days.get(0).getDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(days.get(1).getDayNo()).isEqualTo(2);
        assertThat(days.get(1).getDate()).isEqualTo(LocalDate.of(2026, 7, 11));
        assertThat(days.get(2).getDayNo()).isEqualTo(3);
        assertThat(days.get(2).getDate()).isEqualTo(LocalDate.of(2026, 7, 12));
    }

    @Test
    void createTour_singleDayTour_createsOneDayOnly() {
        setUpSecurityContext("john@example.com");

        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 1))
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tourService.createTour(request);

        ArgumentCaptor<Tour> tourCaptor = ArgumentCaptor.forClass(Tour.class);
        verify(tourRepository).save(tourCaptor.capture());

        Tour savedTour = tourCaptor.getValue();
        assertThat(savedTour.getDays()).hasSize(1);
        assertThat(savedTour.getDays().get(0).getDayNo()).isEqualTo(1);
        assertThat(savedTour.getDays().get(0).getDate()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    void createTour_daysAreLinkedToTour() {
        setUpSecurityContext("john@example.com");

        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 2))
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tourService.createTour(request);

        ArgumentCaptor<Tour> tourCaptor = ArgumentCaptor.forClass(Tour.class);
        verify(tourRepository).save(tourCaptor.capture());

        Tour savedTour = tourCaptor.getValue();
        savedTour.getDays().forEach(day ->
                assertThat(day.getTour()).isSameAs(savedTour)
        );
    }

    @Test
    void createTour_userNotFound_throwsException() {
        setUpSecurityContext("nonexistent@example.com");

        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build();

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tourService.createTour(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Authenticated user not found");

        verify(tourRepository, never()).save(any());
    }

    @Test
    void createTour_usesAuthenticatedUserNotRequestData() {
        setUpSecurityContext("john@example.com");

        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 1))
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tourService.createTour(request);

        ArgumentCaptor<Tour> tourCaptor = ArgumentCaptor.forClass(Tour.class);
        verify(tourRepository).save(tourCaptor.capture());
        assertThat(tourCaptor.getValue().getUser()).isSameAs(testUser);

        verify(userRepository).findByEmail("john@example.com");
    }

    // ==================== getTourById ====================

    @Test
    void getTourById_existingTour_returnsTourResponse() {
        when(tourRepository.findById(tourId)).thenReturn(Optional.of(testTour));

        TourResponse response = tourService.getTourById(tourId);

        assertThat(response.getTourId()).isEqualTo(tourId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getStartDay()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.getEndDay()).isEqualTo(LocalDate.of(2026, 7, 3));
        assertThat(response.getDays()).hasSize(3);
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void getTourById_existingTour_daysMappedCorrectly() {
        when(tourRepository.findById(tourId)).thenReturn(Optional.of(testTour));

        TourResponse response = tourService.getTourById(tourId);

        assertThat(response.getDays().get(0).getDayNo()).isEqualTo(1);
        assertThat(response.getDays().get(0).getDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.getDays().get(0).getTourId()).isEqualTo(tourId);
        assertThat(response.getDays().get(0).getStops()).isEmpty();
    }

    @Test
    void getTourById_nonExistingTour_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(tourRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tourService.getTourById(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tour not found with id: " + nonExistentId);
    }

    // ==================== getToursByUserId ====================

    @Test
    void getToursByUserId_userHasTours_returnsList() {
        setUpSecurityContext("john@example.com");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(tourRepository.findByUser_Id(userId)).thenReturn(List.of(testTour));

        List<TourResponse> responses = tourService.getToursByUserId(null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTourId()).isEqualTo(tourId);
    }

    @Test
    void getToursByUserId_userHasNoTours_returnsEmptyList() {
        setUpSecurityContext("john@example.com");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(tourRepository.findByUser_Id(userId)).thenReturn(Collections.emptyList());

        List<TourResponse> responses = tourService.getToursByUserId(null);

        assertThat(responses).isEmpty();
    }

    @Test
    void getToursByUserId_usesAuthenticatedUserIgnoresParameter() {
        setUpSecurityContext("john@example.com");

        UUID differentUserId = UUID.randomUUID();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(tourRepository.findByUser_Id(userId)).thenReturn(List.of(testTour));

        List<TourResponse> responses = tourService.getToursByUserId(differentUserId);

        verify(tourRepository).findByUser_Id(userId);
        verify(tourRepository, never()).findByUser_Id(differentUserId);
        assertThat(responses).hasSize(1);
    }

    @Test
    void getToursByUserId_userNotAuthenticated_throwsException() {
        setUpSecurityContext("nonexistent@example.com");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tourService.getToursByUserId(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Authenticated user not found");
    }

    // ==================== updateTour ====================

    @Test
    void updateTour_sameDates_doesNotRegenerateDays() {
        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build();

        when(tourRepository.findById(tourId)).thenReturn(Optional.of(testTour));
        when(tourRepository.save(any(Tour.class))).thenReturn(testTour);

        TourResponse response = tourService.updateTour(tourId, request);

        assertThat(response.getDays()).hasSize(3);
    }

    @Test
    void updateTour_differentDates_regeneratesDays() {
        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 8, 1))
                .endDay(LocalDate.of(2026, 8, 5))
                .build();

        when(tourRepository.findById(tourId)).thenReturn(Optional.of(testTour));
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tourService.updateTour(tourId, request);

        ArgumentCaptor<Tour> tourCaptor = ArgumentCaptor.forClass(Tour.class);
        verify(tourRepository).save(tourCaptor.capture());

        Tour savedTour = tourCaptor.getValue();
        assertThat(savedTour.getDays()).hasSize(5);
        assertThat(savedTour.getStartDay()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(savedTour.getEndDay()).isEqualTo(LocalDate.of(2026, 8, 5));
    }

    @Test
    void updateTour_differentDates_newDaysHaveCorrectDatesAndNumbers() {
        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 8, 10))
                .endDay(LocalDate.of(2026, 8, 12))
                .build();

        when(tourRepository.findById(tourId)).thenReturn(Optional.of(testTour));
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tourService.updateTour(tourId, request);

        ArgumentCaptor<Tour> tourCaptor = ArgumentCaptor.forClass(Tour.class);
        verify(tourRepository).save(tourCaptor.capture());

        List<Day> days = tourCaptor.getValue().getDays();
        assertThat(days.get(0).getDayNo()).isEqualTo(1);
        assertThat(days.get(0).getDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(days.get(1).getDayNo()).isEqualTo(2);
        assertThat(days.get(1).getDate()).isEqualTo(LocalDate.of(2026, 8, 11));
        assertThat(days.get(2).getDayNo()).isEqualTo(3);
        assertThat(days.get(2).getDate()).isEqualTo(LocalDate.of(2026, 8, 12));
    }

    @Test
    void updateTour_onlyStartDateChanged_regeneratesDays() {
        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 2))
                .endDay(LocalDate.of(2026, 7, 3))
                .build();

        when(tourRepository.findById(tourId)).thenReturn(Optional.of(testTour));
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tourService.updateTour(tourId, request);

        ArgumentCaptor<Tour> tourCaptor = ArgumentCaptor.forClass(Tour.class);
        verify(tourRepository).save(tourCaptor.capture());

        assertThat(tourCaptor.getValue().getDays()).hasSize(2);
    }

    @Test
    void updateTour_onlyEndDateChanged_regeneratesDays() {
        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 5))
                .build();

        when(tourRepository.findById(tourId)).thenReturn(Optional.of(testTour));
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tourService.updateTour(tourId, request);

        ArgumentCaptor<Tour> tourCaptor = ArgumentCaptor.forClass(Tour.class);
        verify(tourRepository).save(tourCaptor.capture());

        assertThat(tourCaptor.getValue().getDays()).hasSize(5);
    }

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
                .hasMessageContaining("Tour not found with id: " + nonExistentId);
    }

    // ==================== deleteTour ====================

    @Test
    void deleteTour_existingTour_deletesSuccessfully() {
        when(tourRepository.findById(tourId)).thenReturn(Optional.of(testTour));

        tourService.deleteTour(tourId);

        verify(tourRepository).delete(testTour);
    }

    @Test
    void deleteTour_nonExistingTour_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(tourRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tourService.deleteTour(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tour not found with id: " + nonExistentId);

        verify(tourRepository, never()).delete(any());
    }

    // ==================== mapToResponse ====================

    @Test
    void createTour_responseContainsAllFields() {
        setUpSecurityContext("john@example.com");

        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(tourRepository.save(any(Tour.class))).thenReturn(testTour);

        TourResponse response = tourService.createTour(request);

        assertThat(response.getTourId()).isNotNull();
        assertThat(response.getUserId()).isNotNull();
        assertThat(response.getStartDay()).isNotNull();
        assertThat(response.getEndDay()).isNotNull();
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
        assertThat(response.getDays()).isNotNull();
    }

    @Test
    void getTourById_tourWithNoDays_returnsEmptyDaysList() {
        Tour tourWithNoDays = Tour.builder()
                .tourId(tourId)
                .user(testUser)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 1))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .days(null)
                .build();

        when(tourRepository.findById(tourId)).thenReturn(Optional.of(tourWithNoDays));

        TourResponse response = tourService.getTourById(tourId);

        assertThat(response.getDays()).isEmpty();
    }
}
