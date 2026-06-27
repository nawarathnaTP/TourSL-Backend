package com.tourplanner.planning.tour.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tourplanner.planning.auth.exception.GlobalExceptionHandler;
import com.tourplanner.planning.auth.security.JwtUtil;
import com.tourplanner.planning.tour.dto.DayResponse;
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

    private ObjectMapper objectMapper;
    private UUID tourId;
    private UUID userId;
    private TourResponse testTourResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        tourId = UUID.randomUUID();
        userId = UUID.randomUUID();

        DayResponse dayResponse = DayResponse.builder()
                .dayId(UUID.randomUUID())
                .tourId(tourId)
                .dayNo(1)
                .date(LocalDate.of(2026, 7, 1))
                .stops(Collections.emptyList())
                .build();

        testTourResponse = TourResponse.builder()
                .tourId(tourId)
                .userId(userId)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .days(List.of(dayResponse))
                .build();
    }

    // ==================== POST /api/tours ====================

    @Test
    void createTour_validRequest_returns201() throws Exception {
        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build();

        when(tourService.createTour(any(TourRequest.class))).thenReturn(testTourResponse);

        mockMvc.perform(post("/api/tours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tourId").value(tourId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.startDay").value("2026-07-01"))
                .andExpect(jsonPath("$.endDay").value("2026-07-03"))
                .andExpect(jsonPath("$.days").isArray())
                .andExpect(jsonPath("$.days.length()").value(1));
    }

    @Test
    void createTour_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/tours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTour_serviceThrowsException_returns500() throws Exception {
        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build();

        when(tourService.createTour(any(TourRequest.class)))
                .thenThrow(new RuntimeException("Authenticated user not found"));

        mockMvc.perform(post("/api/tours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // ==================== GET /api/tours/{tourId} ====================

    @Test
    void getTourById_existingTour_returns200() throws Exception {
        when(tourService.getTourById(tourId)).thenReturn(testTourResponse);

        mockMvc.perform(get("/api/tours/{tourId}", tourId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tourId").value(tourId.toString()))
                .andExpect(jsonPath("$.startDay").value("2026-07-01"))
                .andExpect(jsonPath("$.endDay").value("2026-07-03"))
                .andExpect(jsonPath("$.days").isArray());
    }

    @Test
    void getTourById_nonExistingTour_returns500() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(tourService.getTourById(nonExistentId))
                .thenThrow(new RuntimeException("Tour not found with id: " + nonExistentId));

        mockMvc.perform(get("/api/tours/{tourId}", nonExistentId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getTourById_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/tours/{tourId}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET /api/tours/my-tours ====================

    @Test
    void getMyTours_userHasTours_returns200WithList() throws Exception {
        when(tourService.getToursByUserId(null)).thenReturn(List.of(testTourResponse));

        mockMvc.perform(get("/api/tours/my-tours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tourId").value(tourId.toString()));
    }

    @Test
    void getMyTours_userHasNoTours_returns200WithEmptyList() throws Exception {
        when(tourService.getToursByUserId(null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/tours/my-tours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== PUT /api/tours/{tourId} ====================

    @Test
    void updateTour_validRequest_returns200() throws Exception {
        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 8, 1))
                .endDay(LocalDate.of(2026, 8, 5))
                .build();

        TourResponse updatedResponse = TourResponse.builder()
                .tourId(tourId)
                .userId(userId)
                .startDay(LocalDate.of(2026, 8, 1))
                .endDay(LocalDate.of(2026, 8, 5))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .days(Collections.emptyList())
                .build();

        when(tourService.updateTour(eq(tourId), any(TourRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/tours/{tourId}", tourId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDay").value("2026-08-01"))
                .andExpect(jsonPath("$.endDay").value("2026-08-05"));
    }

    @Test
    void updateTour_nonExistingTour_returns500() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        TourRequest request = TourRequest.builder()
                .startDay(LocalDate.of(2026, 8, 1))
                .endDay(LocalDate.of(2026, 8, 5))
                .build();

        when(tourService.updateTour(eq(nonExistentId), any(TourRequest.class)))
                .thenThrow(new RuntimeException("Tour not found"));

        mockMvc.perform(put("/api/tours/{tourId}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // ==================== DELETE /api/tours/{tourId} ====================

    @Test
    void deleteTour_existingTour_returns204() throws Exception {
        doNothing().when(tourService).deleteTour(tourId);

        mockMvc.perform(delete("/api/tours/{tourId}", tourId))
                .andExpect(status().isNoContent());

        verify(tourService).deleteTour(tourId);
    }

    @Test
    void deleteTour_nonExistingTour_returns500() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        doThrow(new RuntimeException("Tour not found")).when(tourService).deleteTour(nonExistentId);

        mockMvc.perform(delete("/api/tours/{tourId}", nonExistentId))
                .andExpect(status().isInternalServerError());
    }
}
