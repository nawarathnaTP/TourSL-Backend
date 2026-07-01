package com.tourplanner.planning.stop.controller;

import com.tourplanner.planning.auth.exception.GlobalExceptionHandler;
import com.tourplanner.planning.auth.security.JwtUtil;
import com.tourplanner.planning.stop.dto.StopRequest;
import com.tourplanner.planning.stop.dto.StopResponse;
import com.tourplanner.planning.stop.service.StopService;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StopController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class StopControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StopService stopService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UUID stopId;
    private UUID dayId;
    private UUID locationId;
    private StopResponse stopResponse;

    @BeforeEach
    void setUp() {
        stopId = UUID.randomUUID();
        dayId = UUID.randomUUID();
        locationId = UUID.randomUUID();

        stopResponse = StopResponse.builder()
                .stopId(stopId)
                .dayId(dayId)
                .locationId(locationId)
                .stopOrder(1)
                .duration(120)
                .activities(Collections.emptyList())
                .build();
    }

    // Verifies that a valid POST request creates a stop and returns 201 CREATED
    @Test
    void addStop_validRequest_returns201() throws Exception {
        when(stopService.addStop(any(StopRequest.class))).thenReturn(stopResponse);

        mockMvc.perform(post("/api/stops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayId\":\"" + dayId + "\",\"location\":{\"externalId\":\"ext-123\",\"placeName\":\"Sigiriya\",\"latitude\":7.957,\"longitude\":80.760},\"stopOrder\":1,\"duration\":120}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stopId").value(stopId.toString()))
                .andExpect(jsonPath("$.dayId").value(dayId.toString()))
                .andExpect(jsonPath("$.locationId").value(locationId.toString()))
                .andExpect(jsonPath("$.stopOrder").value(1))
                .andExpect(jsonPath("$.duration").value(120));

        verify(stopService).addStop(any(StopRequest.class));
    }

    // Verifies that GET by stop ID returns the correct stop with 200 OK
    @Test
    void getStopById_existingStop_returns200() throws Exception {
        when(stopService.getStopById(stopId)).thenReturn(stopResponse);

        mockMvc.perform(get("/api/stops/{stopId}", stopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stopId").value(stopId.toString()))
                .andExpect(jsonPath("$.stopOrder").value(1));

        verify(stopService).getStopById(stopId);
    }

    // Verifies that GET by non-existent stop ID propagates a RuntimeException
    @Test
    void getStopById_nonExistingStop_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(stopService.getStopById(nonExistentId))
                .thenThrow(new RuntimeException("Stop not found with id: " + nonExistentId));

        assertThatThrownBy(() -> mockMvc.perform(get("/api/stops/{stopId}", nonExistentId)))
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stop not found with id:");
    }

    // Verifies that an invalid UUID path variable returns 400
    @Test
    void getStopById_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/stops/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    // Verifies that GET /day/{dayId} returns all stops for a day
    @Test
    void getStopsByDayId_returns200WithStopList() throws Exception {
        StopResponse stop2 = StopResponse.builder()
                .stopId(UUID.randomUUID())
                .dayId(dayId)
                .locationId(UUID.randomUUID())
                .stopOrder(2)
                .duration(60)
                .activities(Collections.emptyList())
                .build();

        when(stopService.getStopsByDayId(dayId)).thenReturn(List.of(stopResponse, stop2));

        mockMvc.perform(get("/api/stops/day/{dayId}", dayId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].stopOrder").value(1))
                .andExpect(jsonPath("$[1].stopOrder").value(2));
    }

    // Verifies that GET /day/{dayId} returns an empty list when no stops exist
    @Test
    void getStopsByDayId_noStops_returns200WithEmptyList() throws Exception {
        when(stopService.getStopsByDayId(dayId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/stops/day/{dayId}", dayId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // Verifies that PUT updates a stop and returns 200 OK
    @Test
    void updateStop_validRequest_returns200() throws Exception {
        StopResponse updatedResponse = StopResponse.builder()
                .stopId(stopId)
                .dayId(dayId)
                .locationId(locationId)
                .stopOrder(1)
                .duration(90)
                .activities(Collections.emptyList())
                .build();

        when(stopService.updateStop(eq(stopId), any(StopRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/stops/{stopId}", stopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duration\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration").value(90));

        verify(stopService).updateStop(eq(stopId), any(StopRequest.class));
    }

    // Verifies that PUT /day/{dayId}/reorder reorders stops and returns 200 OK
    @Test
    void reorderStops_validRequest_returns200() throws Exception {
        UUID stopId2 = UUID.randomUUID();
        List<StopResponse> reorderedList = List.of(
                StopResponse.builder().stopId(stopId2).dayId(dayId).stopOrder(1)
                        .activities(Collections.emptyList()).build(),
                StopResponse.builder().stopId(stopId).dayId(dayId).stopOrder(2)
                        .activities(Collections.emptyList()).build()
        );

        when(stopService.reorderStops(eq(dayId), anyList())).thenReturn(reorderedList);

        mockMvc.perform(put("/api/stops/day/{dayId}/reorder", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stopIds\":[\"" + stopId2 + "\",\"" + stopId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].stopOrder").value(1))
                .andExpect(jsonPath("$[1].stopOrder").value(2));
    }

    // Verifies that PUT /{stopId}/move moves a stop and returns 200 OK
    @Test
    void moveStop_validRequest_returns200() throws Exception {
        UUID targetDayId = UUID.randomUUID();
        StopResponse movedResponse = StopResponse.builder()
                .stopId(stopId)
                .dayId(targetDayId)
                .locationId(locationId)
                .stopOrder(1)
                .duration(120)
                .activities(Collections.emptyList())
                .build();

        when(stopService.moveStop(eq(stopId), eq(targetDayId), eq(1))).thenReturn(movedResponse);

        mockMvc.perform(put("/api/stops/{stopId}/move", stopId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetDayId\":\"" + targetDayId + "\",\"targetOrder\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayId").value(targetDayId.toString()))
                .andExpect(jsonPath("$.stopOrder").value(1));
    }

    // Verifies that DELETE returns 204 NO CONTENT on successful deletion
    @Test
    void deleteStop_existingStop_returns204() throws Exception {
        doNothing().when(stopService).deleteStop(stopId);

        mockMvc.perform(delete("/api/stops/{stopId}", stopId))
                .andExpect(status().isNoContent());

        verify(stopService).deleteStop(stopId);
    }

    // Verifies that DELETE for a non-existent stop propagates the exception
    @Test
    void deleteStop_nonExistingStop_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        doThrow(new RuntimeException("Stop not found with id: " + nonExistentId))
                .when(stopService).deleteStop(nonExistentId);

        assertThatThrownBy(() -> mockMvc.perform(delete("/api/stops/{stopId}", nonExistentId)))
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stop not found with id:");
    }
}
