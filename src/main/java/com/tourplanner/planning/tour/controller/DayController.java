package com.tourplanner.planning.tour.controller;

import com.tourplanner.planning.tour.dto.DayRequest;
import com.tourplanner.planning.tour.dto.DayResponse;
import com.tourplanner.planning.tour.service.DayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/days")
@RequiredArgsConstructor
public class DayController {

    private final DayService dayService;

    @GetMapping("/{dayId}")
    public ResponseEntity<DayResponse> getDayById(@PathVariable UUID dayId) {
        return ResponseEntity.ok(dayService.getDayById(dayId));
    }

    @GetMapping("/tour/{tourId}")
    public ResponseEntity<List<DayResponse>> getDaysByTourId(@PathVariable UUID tourId) {
        return ResponseEntity.ok(dayService.getDaysByTourId(tourId));
    }

    @PutMapping("/{dayId}")
    public ResponseEntity<DayResponse> updateDay(@PathVariable UUID dayId, @RequestBody DayRequest request) {
        return ResponseEntity.ok(dayService.updateDay(dayId, request));
    }

    @PutMapping("/{dayId}/clear")
    public ResponseEntity<DayResponse> clearDay(@PathVariable UUID dayId) {
        return ResponseEntity.ok(dayService.clearDay(dayId));
    }
}
