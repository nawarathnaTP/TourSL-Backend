package com.tourplanner.planning.stop.controller;

import com.tourplanner.planning.auth.exception.GlobalExceptionHandler;
import com.tourplanner.planning.auth.security.JwtUtil;
import com.tourplanner.planning.stop.dto.ActivityRequest;
import com.tourplanner.planning.stop.dto.ActivityResponse;
import com.tourplanner.planning.stop.service.ActivityService;
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

@WebMvcTest(ActivityController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityService activityService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UUID activityId;
    private UUID stopId;
    private ActivityResponse activityResponse;

    @BeforeEach
    void setUp() {
        activityId = UUID.randomUUID();
        stopId = UUID.randomUUID();

        activityResponse = ActivityResponse.builder()
                .activityId(activityId)
                .stopId(stopId)
                .duration(60)
                .description("Visit temple")
                .bookingId(null)
                .build();
    }

    // Verifies that a valid POST request creates an activity and returns 201 CREATED
    @Test
    void addActivity_validRequest_returns201() throws Exception {
        when(activityService.addActivity(any(ActivityRequest.class))).thenReturn(activityResponse);

        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stopId\":\"" + stopId + "\",\"duration\":60,\"description\":\"Visit temple\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activityId").value(activityId.toString()))
                .andExpect(jsonPath("$.stopId").value(stopId.toString()))
                .andExpect(jsonPath("$.duration").value(60))
                .andExpect(jsonPath("$.description").value("Visit temple"));

        verify(activityService).addActivity(any(ActivityRequest.class));
    }

    // Verifies that GET by activity ID returns the correct activity with 200 OK
    @Test
    void getActivityById_existingActivity_returns200() throws Exception {
        when(activityService.getActivityById(activityId)).thenReturn(activityResponse);

        mockMvc.perform(get("/api/activities/{activityId}", activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityId").value(activityId.toString()))
                .andExpect(jsonPath("$.description").value("Visit temple"));

        verify(activityService).getActivityById(activityId);
    }

    // Verifies that GET by non-existent activity ID propagates a RuntimeException
    @Test
    void getActivityById_nonExistingActivity_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(activityService.getActivityById(nonExistentId))
                .thenThrow(new RuntimeException("Activity not found with id: " + nonExistentId));

        assertThatThrownBy(() -> mockMvc.perform(get("/api/activities/{activityId}", nonExistentId)))
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Activity not found with id:");
    }

    // Verifies that an invalid UUID path variable returns 400
    @Test
    void getActivityById_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/activities/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    // Verifies that GET /stop/{stopId} returns all activities for a stop
    @Test
    void getActivitiesByStopId_returns200WithActivityList() throws Exception {
        ActivityResponse activity2 = ActivityResponse.builder()
                .activityId(UUID.randomUUID())
                .stopId(stopId)
                .duration(30)
                .description("Take photos")
                .build();

        when(activityService.getActivitiesByStopId(stopId)).thenReturn(List.of(activityResponse, activity2));

        mockMvc.perform(get("/api/activities/stop/{stopId}", stopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].description").value("Visit temple"))
                .andExpect(jsonPath("$[1].description").value("Take photos"));
    }

    // Verifies that GET /stop/{stopId} returns an empty list when no activities exist
    @Test
    void getActivitiesByStopId_noActivities_returns200WithEmptyList() throws Exception {
        when(activityService.getActivitiesByStopId(stopId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/activities/stop/{stopId}", stopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // Verifies that PUT updates an activity and returns 200 OK
    @Test
    void updateActivity_validRequest_returns200() throws Exception {
        ActivityResponse updatedResponse = ActivityResponse.builder()
                .activityId(activityId)
                .stopId(stopId)
                .duration(90)
                .description("Explore ruins")
                .build();

        when(activityService.updateActivity(eq(activityId), any(ActivityRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/activities/{activityId}", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duration\":90,\"description\":\"Explore ruins\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration").value(90))
                .andExpect(jsonPath("$.description").value("Explore ruins"));

        verify(activityService).updateActivity(eq(activityId), any(ActivityRequest.class));
    }

    // Verifies that PUT for a non-existent activity propagates the exception
    @Test
    void updateActivity_nonExistingActivity_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(activityService.updateActivity(eq(nonExistentId), any(ActivityRequest.class)))
                .thenThrow(new RuntimeException("Activity not found with id: " + nonExistentId));

        assertThatThrownBy(() -> mockMvc.perform(put("/api/activities/{activityId}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duration\":30}")))
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Activity not found with id:");
    }

    // Verifies that DELETE returns 204 NO CONTENT on successful deletion
    @Test
    void deleteActivity_existingActivity_returns204() throws Exception {
        doNothing().when(activityService).deleteActivity(activityId);

        mockMvc.perform(delete("/api/activities/{activityId}", activityId))
                .andExpect(status().isNoContent());

        verify(activityService).deleteActivity(activityId);
    }

    // Verifies that DELETE for a non-existent activity propagates the exception
    @Test
    void deleteActivity_nonExistingActivity_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        doThrow(new RuntimeException("Activity not found with id: " + nonExistentId))
                .when(activityService).deleteActivity(nonExistentId);

        assertThatThrownBy(() -> mockMvc.perform(delete("/api/activities/{activityId}", nonExistentId)))
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Activity not found with id:");
    }

    // SECURITY: Verifies that null fields are excluded from the JSON response
    @Test
    void getActivityById_responseExcludesNullFields() throws Exception {
        when(activityService.getActivityById(activityId)).thenReturn(activityResponse);

        mockMvc.perform(get("/api/activities/{activityId}", activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").doesNotExist());
    }
}
