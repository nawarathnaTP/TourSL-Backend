package com.tourplanner.planning.tour.controller;

import com.tourplanner.planning.auth.exception.GlobalExceptionHandler;
import com.tourplanner.planning.auth.security.JwtUtil;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
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

    private UUID dayId;
    private UUID tourId;
    private DayResponse dayResponse;

    @BeforeEach
    void setUp() {
        dayId = UUID.randomUUID();
        tourId = UUID.randomUUID();

        dayResponse = DayResponse.builder()
                .dayId(dayId)
                .tourId(tourId)
                .dayNo(1)
                .date(LocalDate.of(2026, 7, 1))
                .lodgingId(null)
                .stops(Collections.emptyList())
                .build();
    }

    // Verifies that GET by day ID returns the correct day with 200 OK
    @Test
    void getDayById_existingDay_returns200() throws Exception {
        when(dayService.getDayById(dayId)).thenReturn(dayResponse);

        mockMvc.perform(get("/api/days/{dayId}", dayId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayId").value(dayId.toString()))
                .andExpect(jsonPath("$.tourId").value(tourId.toString()))
                .andExpect(jsonPath("$.dayNo").value(1))
                .andExpect(jsonPath("$.date").value("2026-07-01"));

        verify(dayService).getDayById(dayId);
    }

    // Verifies that GET by non-existent day ID propagates a RuntimeException from the service
    @Test
    void getDayById_nonExistingDay_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(dayService.getDayById(nonExistentId))
                .thenThrow(new RuntimeException("Day not found with id: " + nonExistentId));

        assertThatThrownBy(() -> mockMvc.perform(get("/api/days/{dayId}", nonExistentId)))
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Day not found with id:");
    }

    // Verifies that an invalid UUID path variable returns 400 BAD REQUEST
    @Test
    void getDayById_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/days/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    // Verifies that GET /tour/{tourId} returns all days for the specified tour
    @Test
    void getDaysByTourId_returns200WithDayList() throws Exception {
        DayResponse day2 = DayResponse.builder()
                .dayId(UUID.randomUUID())
                .tourId(tourId)
                .dayNo(2)
                .date(LocalDate.of(2026, 7, 2))
                .stops(Collections.emptyList())
                .build();

        when(dayService.getDaysByTourId(tourId)).thenReturn(List.of(dayResponse, day2));

        mockMvc.perform(get("/api/days/tour/{tourId}", tourId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dayNo").value(1))
                .andExpect(jsonPath("$[1].dayNo").value(2));

        verify(dayService).getDaysByTourId(tourId);
    }

    // Verifies that GET /tour/{tourId} returns an empty list when the tour has no days
    @Test
    void getDaysByTourId_noDays_returns200WithEmptyList() throws Exception {
        when(dayService.getDaysByTourId(tourId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/days/tour/{tourId}", tourId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // Verifies that PUT updates a day's lodging and returns 200 OK
    @Test
    void updateDay_validRequest_returns200() throws Exception {
        UUID lodgingId = UUID.randomUUID();

        DayResponse updatedResponse = DayResponse.builder()
                .dayId(dayId)
                .tourId(tourId)
                .dayNo(1)
                .date(LocalDate.of(2026, 7, 1))
                .lodgingId(lodgingId)
                .stops(Collections.emptyList())
                .build();

        when(dayService.updateDay(eq(dayId), any())).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/days/{dayId}", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lodgingId\":\"" + lodgingId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lodgingId").value(lodgingId.toString()));

        verify(dayService).updateDay(eq(dayId), any());
    }

    // Verifies that PUT for a non-existent day propagates a RuntimeException from the service
    @Test
    void updateDay_nonExistingDay_throwsException() {
        UUID nonExistentId = UUID.randomUUID();

        when(dayService.updateDay(eq(nonExistentId), any()))
                .thenThrow(new RuntimeException("Day not found with id: " + nonExistentId));

        assertThatThrownBy(() -> mockMvc.perform(put("/api/days/{dayId}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lodgingId\":\"" + UUID.randomUUID() + "\"}")))
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Day not found with id:");
    }

    // Verifies that PUT /{dayId}/clear clears the day and returns 200 OK
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
                .andExpect(jsonPath("$.stops").isEmpty())
                .andExpect(jsonPath("$.lodgingId").doesNotExist());

        verify(dayService).clearDay(dayId);
    }

    // Verifies that PUT /{dayId}/clear for a non-existent day propagates a RuntimeException from the service
    @Test
    void clearDay_nonExistingDay_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(dayService.clearDay(nonExistentId))
                .thenThrow(new RuntimeException("Day not found with id: " + nonExistentId));

        assertThatThrownBy(() -> mockMvc.perform(put("/api/days/{dayId}/clear", nonExistentId)))
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Day not found with id:");
    }

    // SECURITY: Verifies that null fields are excluded from the JSON response (non_null inclusion)
    @Test
    void getDayById_responseExcludesNullFields() throws Exception {
        DayResponse responseWithNullLodging = DayResponse.builder()
                .dayId(dayId)
                .tourId(tourId)
                .dayNo(1)
                .date(LocalDate.of(2026, 7, 1))
                .lodgingId(null)
                .stops(Collections.emptyList())
                .build();

        when(dayService.getDayById(dayId)).thenReturn(responseWithNullLodging);

        mockMvc.perform(get("/api/days/{dayId}", dayId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lodgingId").doesNotExist());
    }
}
