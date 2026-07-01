package com.tourplanner.planning.stop.controller;

import com.tourplanner.planning.stop.dto.MoveStopRequest;
import com.tourplanner.planning.stop.dto.ReorderStopsRequest;
import com.tourplanner.planning.stop.dto.StopRequest;
import com.tourplanner.planning.stop.dto.StopResponse;
import com.tourplanner.planning.stop.service.StopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stops")
@RequiredArgsConstructor
public class StopController {

    private final StopService stopService;

    @PostMapping
    public ResponseEntity<StopResponse> addStop(@RequestBody StopRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stopService.addStop(request));
    }

    @GetMapping("/{stopId}")
    public ResponseEntity<StopResponse> getStopById(@PathVariable UUID stopId) {
        return ResponseEntity.ok(stopService.getStopById(stopId));
    }

    @GetMapping("/day/{dayId}")
    public ResponseEntity<List<StopResponse>> getStopsByDayId(@PathVariable UUID dayId) {
        return ResponseEntity.ok(stopService.getStopsByDayId(dayId));
    }

    @PutMapping("/{stopId}")
    public ResponseEntity<StopResponse> updateStop(@PathVariable UUID stopId, @RequestBody StopRequest request) {
        return ResponseEntity.ok(stopService.updateStop(stopId, request));
    }

    @PutMapping("/day/{dayId}/reorder")
    public ResponseEntity<List<StopResponse>> reorderStops(@PathVariable UUID dayId, @RequestBody ReorderStopsRequest request) {
        return ResponseEntity.ok(stopService.reorderStops(dayId, request.getStopIds()));
    }

    @PutMapping("/{stopId}/move")
    public ResponseEntity<StopResponse> moveStop(@PathVariable UUID stopId, @RequestBody MoveStopRequest request) {
        return ResponseEntity.ok(stopService.moveStop(stopId, request.getTargetDayId(), request.getTargetOrder()));
    }

    @DeleteMapping("/{stopId}")
    public ResponseEntity<Void> deleteStop(@PathVariable UUID stopId) {
        stopService.deleteStop(stopId);
        return ResponseEntity.noContent().build();
    }
}
