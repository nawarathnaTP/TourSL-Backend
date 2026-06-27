package com.tourplanner.planning.tour.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tourplanner.planning.auth.exception.GlobalExceptionHandler;
import com.tourplanner.planning.auth.security.JwtUtil;
import com.tourplanner.planning.tour.dto.DayRequest;
import com.tourplanner.planning.tour.dto.DayResponse;
import com.tourplanner.planning.tour.service.DayService;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DayController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class DayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DayService dayService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private UUID dayId;
    private UUID tourId;
    private DayResponse testDayResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        dayId = UUID.randomUUID();
        tourId = UUID.randomUUID();

        testDayResponse = DayResponse.builder()
                .dayId(dayId)
                .tourId(tourId)
                .dayNo(1)
                .date(LocalDate.of(2026, 7, 1))
                .lodgingId(null)
                .stops(Collections.emptyList())
                .build();
    }

    // ==================== GET /api/days/{dayId} ====================

    @Test
    void getDayById_existingDay_returns200() throws Exception {
        when(dayService.getDayById(dayId)).thenReturn(testDayResponse);

        mockMvc.perform(get("/api/days/{dayId}", dayId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayId").value(dayId.toString()))
                .andExpect(jsonPath("$.tourId").value(tourId.toString()))
                .andExpect(jsonPath("$.dayNo").value(1))
                .andExpect(jsonPath("$.date").value("2026-07-01"))
                .andExpect(jsonPath("$.stops").isArray());
    }

    @Test
    void getDayById_nonExistingDay_returns500() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(dayService.getDayById(nonExistentId))
                .thenThrow(new RuntimeException("Day not found with id: " + nonExistentId));

        mockMvc.perform(get("/api/days/{dayId}", nonExistentId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getDayById_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/days/{dayId}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET /api/days/tour/{tourId} ====================

    @Test
    void getDaysByTourId_tourHasDays_returns200WithList() throws Exception {
        DayResponse day2 = DayResponse.builder()
                .dayId(UUID.randomUUID())
                .tourId(tourId)
                .dayNo(2)
                .date(LocalDate.of(2026, 7, 2))
                .stops(Collections.emptyList())
                .build();

        when(dayService.getDaysByTourId(tourId)).thenReturn(List.of(testDayResponse, day2));

        mockMvc.perform(get("/api/days/tour/{tourId}", tourId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dayNo").value(1))
                .andExpect(jsonPath("$[1].dayNo").value(2));
    }

    @Test
    void getDaysByTourId_tourHasNoDays_returns200WithEmptyList() throws Exception {
        when(dayService.getDaysByTourId(tourId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/days/tour/{tourId}", tourId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== PUT /api/days/{dayId} ====================

    @Test
    void updateDay_withLodgingId_returns200() throws Exception {
        UUID lodgingId = UUID.randomUUID();
        DayRequest request = DayRequest.builder()
                .lodgingId(lodgingId)
                .build();

        DayResponse updatedResponse = DayResponse.builder()
                .dayId(dayId)
                .tourId(tourId)
                .dayNo(1)
                .date(LocalDate.of(2026, 7, 1))
                .lodgingId(lodgingId)
                .stops(Collections.emptyList())
                .build();

        when(dayService.updateDay(eq(dayId), any(DayRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/days/{dayId}", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lodgingId").value(lodgingId.toString()));
    }

    @Test
    void updateDay_nonExistingDay_returns500() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        DayRequest request = DayRequest.builder().build();

        when(dayService.updateDay(eq(nonExistentId), any(DayRequest.class)))
                .thenThrow(new RuntimeException("Day not found"));

        mockMvc.perform(put("/api/days/{dayId}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updateDay_emptyBody_returns400() throws Exception {
        mockMvc.perform(put("/api/days/{dayId}", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    // ==================== PUT /api/days/{dayId}/clear ====================

    @Test
    void clearDay_existingDay_returns200() throws Exception {
        DayResponse clearedResponse = DayResponse.builder()
                .dayId(dayId)
                .tourId(tourId)
                .dayNo(1)
                .date(LocalDate.of(2026, 7, 1))
                .lodgingId(null)
                .stops(Collections.emptyList())
                .build();

        when(dayService.clearDay(dayId)).thenReturn(clearedResponse);

        mockMvc.perform(put("/api/days/{dayId}/clear", dayId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayId").value(dayId.toString()))
                .andExpect(jsonPath("$.lodgingId").isEmpty())
                .andExpect(jsonPath("$.stops").isArray())
                .andExpect(jsonPath("$.stops.length()").value(0));
    }

    @Test
    void clearDay_nonExistingDay_returns500() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(dayService.clearDay(nonExistentId))
                .thenThrow(new RuntimeException("Day not found"));

        mockMvc.perform(put("/api/days/{dayId}/clear", nonExistentId))
                .andExpect(status().isInternalServerError());
    }
}
