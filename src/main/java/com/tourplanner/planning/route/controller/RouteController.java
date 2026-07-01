package com.tourplanner.planning.route.controller;

import com.tourplanner.planning.route.dto.RouteRequest;
import com.tourplanner.planning.route.dto.RouteResponse;
import com.tourplanner.planning.route.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping
    public ResponseEntity<RouteResponse> createRoute(@RequestBody RouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routeService.createRoute(request));
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<RouteResponse> getRouteById(@PathVariable UUID routeId) {
        return ResponseEntity.ok(routeService.getRouteById(routeId));
    }

    @GetMapping("/day/{dayId}")
    public ResponseEntity<List<RouteResponse>> getRoutesByDayId(@PathVariable UUID dayId) {
        return ResponseEntity.ok(routeService.getRoutesByDayId(dayId));
    }

    @PutMapping("/{routeId}")
    public ResponseEntity<RouteResponse> updateRoute(@PathVariable UUID routeId, @RequestBody RouteRequest request) {
        return ResponseEntity.ok(routeService.updateRoute(routeId, request));
    }

    @DeleteMapping("/{routeId}")
    public ResponseEntity<Void> deleteRoute(@PathVariable UUID routeId) {
        routeService.deleteRoute(routeId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/day/{dayId}")
    public ResponseEntity<Void> deleteRoutesForDay(@PathVariable UUID dayId) {
        routeService.deleteRoutesForDay(dayId);
        return ResponseEntity.noContent().build();
    }
}
