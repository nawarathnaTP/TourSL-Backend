package com.tourplanner.planning.stop.controller;

import com.tourplanner.planning.stop.dto.ActivityRequest;
import com.tourplanner.planning.stop.dto.ActivityResponse;
import com.tourplanner.planning.stop.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping
    public ResponseEntity<ActivityResponse> addActivity(@RequestBody ActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.addActivity(request));
    }

    @GetMapping("/{activityId}")
    public ResponseEntity<ActivityResponse> getActivityById(@PathVariable UUID activityId) {
        return ResponseEntity.ok(activityService.getActivityById(activityId));
    }

    @GetMapping("/stop/{stopId}")
    public ResponseEntity<List<ActivityResponse>> getActivitiesByStopId(@PathVariable UUID stopId) {
        return ResponseEntity.ok(activityService.getActivitiesByStopId(stopId));
    }

    @PutMapping("/{activityId}")
    public ResponseEntity<ActivityResponse> updateActivity(@PathVariable UUID activityId, @RequestBody ActivityRequest request) {
        return ResponseEntity.ok(activityService.updateActivity(activityId, request));
    }

    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> deleteActivity(@PathVariable UUID activityId) {
        activityService.deleteActivity(activityId);
        return ResponseEntity.noContent().build();
    }
}
