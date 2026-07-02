package com.tourplanner.planning.route.controller;

import com.tourplanner.planning.auth.exception.GlobalExceptionHandler;
import com.tourplanner.planning.auth.security.JwtUtil;
import com.tourplanner.planning.route.dto.RouteRequest;
import com.tourplanner.planning.route.dto.RouteResponse;
import com.tourplanner.planning.route.dto.TransportOptionResponse;
import com.tourplanner.planning.route.service.RouteService;
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

import java.math.BigDecimal;
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

@WebMvcTest(RouteController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class RouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RouteService routeService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UUID routeId;
    private UUID startStopId;
    private UUID endStopId;
    private UUID dayId;
    private RouteResponse routeResponse;

    @BeforeEach
    void setUp() {
        routeId = UUID.randomUUID();
        startStopId = UUID.randomUUID();
        endStopId = UUID.randomUUID();
        dayId = UUID.randomUUID();

        TransportOptionResponse transport = TransportOptionResponse.builder()
                .transportId(UUID.randomUUID())
                .type("bus")
                .label("Public Bus")
                .build();

        routeResponse = RouteResponse.builder()
                .routeId(routeId)
                .startStopId(startStopId)
                .endStopId(endStopId)
                .transport(transport)
                .distance(BigDecimal.valueOf(25.5))
                .time(45)
                .cost(BigDecimal.valueOf(150))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    // Verifies that a valid POST request creates a route and returns 201 CREATED
    @Test
    void createRoute_validRequest_returns201() throws Exception {
        when(routeService.createRoute(any(RouteRequest.class))).thenReturn(routeResponse);

        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startStopId\":\"" + startStopId + "\",\"endStopId\":\"" + endStopId
                                + "\",\"transportType\":\"bus\",\"transportLabel\":\"Public Bus\""
                                + ",\"distance\":25.5,\"time\":45,\"cost\":150}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routeId").value(routeId.toString()))
                .andExpect(jsonPath("$.startStopId").value(startStopId.toString()))
                .andExpect(jsonPath("$.endStopId").value(endStopId.toString()))
                .andExpect(jsonPath("$.transport.type").value("bus"))
                .andExpect(jsonPath("$.distance").value(25.5))
                .andExpect(jsonPath("$.time").value(45))
                .andExpect(jsonPath("$.cost").value(150));

        verify(routeService).createRoute(any(RouteRequest.class));
    }

    // Verifies that GET by route ID returns the correct route with 200 OK
    @Test
    void getRouteById_existingRoute_returns200() throws Exception {
        when(routeService.getRouteById(routeId)).thenReturn(routeResponse);

        mockMvc.perform(get("/api/routes/{routeId}", routeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(routeId.toString()))
                .andExpect(jsonPath("$.transport.type").value("bus"));

        verify(routeService).getRouteById(routeId);
    }

    // Verifies that GET by non-existent route ID propagates a RuntimeException
    @Test
    void getRouteById_nonExistingRoute_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(routeService.getRouteById(nonExistentId))
                .thenThrow(new RuntimeException("Route not found with id: " + nonExistentId));

        assertThatThrownBy(() -> mockMvc.perform(get("/api/routes/{routeId}", nonExistentId)))
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Route not found with id:");
    }

    // Verifies that an invalid UUID path variable returns 400
    @Test
    void getRouteById_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/routes/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    // Verifies that GET /day/{dayId} returns all routes for a day
    @Test
    void getRoutesByDayId_returns200WithRouteList() throws Exception {
        RouteResponse route2 = RouteResponse.builder()
                .routeId(UUID.randomUUID())
                .startStopId(UUID.randomUUID())
                .endStopId(UUID.randomUUID())
                .distance(BigDecimal.valueOf(10))
                .time(20)
                .cost(BigDecimal.valueOf(50))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(routeService.getRoutesByDayId(dayId)).thenReturn(List.of(routeResponse, route2));

        mockMvc.perform(get("/api/routes/day/{dayId}", dayId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // Verifies that GET /day/{dayId} returns an empty list when no routes exist
    @Test
    void getRoutesByDayId_noRoutes_returns200WithEmptyList() throws Exception {
        when(routeService.getRoutesByDayId(dayId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/routes/day/{dayId}", dayId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // Verifies that PUT updates a route and returns 200 OK
    @Test
    void updateRoute_validRequest_returns200() throws Exception {
        RouteResponse updatedResponse = RouteResponse.builder()
                .routeId(routeId)
                .startStopId(startStopId)
                .endStopId(endStopId)
                .transport(routeResponse.getTransport())
                .distance(BigDecimal.valueOf(30))
                .time(50)
                .cost(BigDecimal.valueOf(200))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(routeService.updateRoute(eq(routeId), any(RouteRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/routes/{routeId}", routeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"distance\":30,\"time\":50,\"cost\":200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distance").value(30))
                .andExpect(jsonPath("$.time").value(50))
                .andExpect(jsonPath("$.cost").value(200));

        verify(routeService).updateRoute(eq(routeId), any(RouteRequest.class));
    }

    // Verifies that DELETE returns 204 NO CONTENT on successful deletion
    @Test
    void deleteRoute_existingRoute_returns204() throws Exception {
        doNothing().when(routeService).deleteRoute(routeId);

        mockMvc.perform(delete("/api/routes/{routeId}", routeId))
                .andExpect(status().isNoContent());

        verify(routeService).deleteRoute(routeId);
    }

    // Verifies that DELETE for a non-existent route propagates the exception
    @Test
    void deleteRoute_nonExistingRoute_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        doThrow(new RuntimeException("Route not found with id: " + nonExistentId))
                .when(routeService).deleteRoute(nonExistentId);

        assertThatThrownBy(() -> mockMvc.perform(delete("/api/routes/{routeId}", nonExistentId)))
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Route not found with id:");
    }

    // Verifies that DELETE /day/{dayId} returns 204 NO CONTENT
    @Test
    void deleteRoutesForDay_returns204() throws Exception {
        doNothing().when(routeService).deleteRoutesForDay(dayId);

        mockMvc.perform(delete("/api/routes/day/{dayId}", dayId))
                .andExpect(status().isNoContent());

        verify(routeService).deleteRoutesForDay(dayId);
    }
}
