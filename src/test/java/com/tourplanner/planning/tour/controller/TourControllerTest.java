package com.tourplanner.planning.tour.controller;

import com.tourplanner.planning.auth.exception.GlobalExceptionHandler;
import com.tourplanner.planning.auth.security.JwtUtil;
import com.tourplanner.planning.tour.dto.TourRequest;
import com.tourplanner.planning.tour.dto.TourResponse;
import com.tourplanner.planning.tour.service.TourService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TourController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class TourControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TourService tourService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UUID tourId;
    private UUID userId;
    private TourResponse tourResponse;

    @BeforeEach
    void setUp() {
        tourId = UUID.randomUUID();
        userId = UUID.randomUUID();

        tourResponse = TourResponse.builder()
                .tourId(tourId)
                .userId(userId)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .days(Collections.emptyList())
                .build();
    }

    // Verifies that a valid POST request creates a tour and returns 201 CREATED
    @Test
    void createTour_validRequest_returns201() throws Exception {
        when(tourService.createTour(any(TourRequest.class))).thenReturn(tourResponse);

        mockMvc.perform(post("/api/tours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDay\":\"2026-07-01\",\"endDay\":\"2026-07-03\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tourId").value(tourId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.startDay").value("2026-07-01"))
                .andExpect(jsonPath("$.endDay").value("2026-07-03"));

        verify(tourService).createTour(any(TourRequest.class));
    }

    // Verifies that GET by tour ID returns the correct tour with 200 OK
    @Test
    void getTourById_existingTour_returns200() throws Exception {
        when(tourService.getTourById(tourId)).thenReturn(tourResponse);

        mockMvc.perform(get("/api/tours/{tourId}", tourId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tourId").value(tourId.toString()))
                .andExpect(jsonPath("$.startDay").value("2026-07-01"))
                .andExpect(jsonPath("$.endDay").value("2026-07-03"));

        verify(tourService).getTourById(tourId);
    }

    // Verifies that GET by non-existent tour ID propagates a RuntimeException from the service
    @Test
    void getTourById_nonExistingTour_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(tourService.getTourById(nonExistentId))
                .thenThrow(new RuntimeException("Tour not found with id: " + nonExistentId));

        assertThatThrownBy(() -> mockMvc.perform(get("/api/tours/{tourId}", nonExistentId)))
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tour not found with id:");
    }

    // Verifies that GET /my-tours returns the authenticated user's tours
    @Test
    void getMyTours_returns200WithTourList() throws Exception {
        TourResponse tour2 = TourResponse.builder()
                .tourId(UUID.randomUUID())
                .userId(userId)
                .startDay(LocalDate.of(2026, 8, 1))
                .endDay(LocalDate.of(2026, 8, 5))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .days(Collections.emptyList())
                .build();

        when(tourService.getToursByUserId(null)).thenReturn(List.of(tourResponse, tour2));

        mockMvc.perform(get("/api/tours/my-tours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tourId").value(tourId.toString()));

        verify(tourService).getToursByUserId(null);
    }

    // Verifies that GET /my-tours returns an empty list when the user has no tours
    @Test
    void getMyTours_noTours_returns200WithEmptyList() throws Exception {
        when(tourService.getToursByUserId(null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/tours/my-tours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // Verifies that PUT updates a tour and returns 200 OK with the updated response
    @Test
    void updateTour_validRequest_returns200() throws Exception {
        TourResponse updatedResponse = TourResponse.builder()
                .tourId(tourId)
                .userId(userId)
                .startDay(LocalDate.of(2026, 7, 10))
                .endDay(LocalDate.of(2026, 7, 15))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .days(Collections.emptyList())
                .build();

        when(tourService.updateTour(eq(tourId), any(TourRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/tours/{tourId}", tourId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDay\":\"2026-07-10\",\"endDay\":\"2026-07-15\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDay").value("2026-07-10"))
                .andExpect(jsonPath("$.endDay").value("2026-07-15"));

        verify(tourService).updateTour(eq(tourId), any(TourRequest.class));
    }

    // Verifies that PUT for a non-existent tour propagates a RuntimeException from the service
    @Test
    void updateTour_nonExistingTour_throwsException() {
        UUID nonExistentId = UUID.randomUUID();

        when(tourService.updateTour(eq(nonExistentId), any(TourRequest.class)))
                .thenThrow(new RuntimeException("Tour not found with id: " + nonExistentId));

        assertThatThrownBy(() -> mockMvc.perform(put("/api/tours/{tourId}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDay\":\"2026-07-01\",\"endDay\":\"2026-07-03\"}")))
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tour not found with id:");
    }

    // Verifies that DELETE returns 204 NO CONTENT on successful deletion
    @Test
    void deleteTour_existingTour_returns204() throws Exception {
        doNothing().when(tourService).deleteTour(tourId);

        mockMvc.perform(delete("/api/tours/{tourId}", tourId))
                .andExpect(status().isNoContent());

        verify(tourService).deleteTour(tourId);
    }

    // Verifies that DELETE for a non-existent tour propagates a RuntimeException from the service
    @Test
    void deleteTour_nonExistingTour_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        doThrow(new RuntimeException("Tour not found with id: " + nonExistentId))
                .when(tourService).deleteTour(nonExistentId);

        assertThatThrownBy(() -> mockMvc.perform(delete("/api/tours/{tourId}", nonExistentId)))
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tour not found with id:");
    }

    // Verifies that an invalid UUID path variable returns 400 BAD REQUEST
    @Test
    void getTourById_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/tours/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    // Verifies that POST with an empty body still reaches the controller (no validation annotations on TourRequest)
    @Test
    void createTour_emptyBody_returns201WhenServiceAccepts() throws Exception {
        when(tourService.createTour(any(TourRequest.class))).thenReturn(tourResponse);

        mockMvc.perform(post("/api/tours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
    }

    // SECURITY: Verifies that null fields are excluded from the JSON response (non_null inclusion)
    @Test
    void createTour_responseExcludesNullFields() throws Exception {
        TourResponse responseWithNulls = TourResponse.builder()
                .tourId(tourId)
                .userId(userId)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .days(Collections.emptyList())
                .build();

        when(tourService.createTour(any(TourRequest.class))).thenReturn(responseWithNulls);

        mockMvc.perform(post("/api/tours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDay\":\"2026-07-01\",\"endDay\":\"2026-07-03\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }
}
