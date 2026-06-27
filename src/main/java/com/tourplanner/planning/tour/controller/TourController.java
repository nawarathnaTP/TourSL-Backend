package com.tourplanner.planning.tour.controller;

import com.tourplanner.planning.tour.dto.TourRequest;
import com.tourplanner.planning.tour.dto.TourResponse;
import com.tourplanner.planning.tour.service.TourService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tours")
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;

    @PostMapping
    public ResponseEntity<TourResponse> createTour(@RequestBody TourRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tourService.createTour(request));
    }

    @GetMapping("/{tourId}")
    public ResponseEntity<TourResponse> getTourById(@PathVariable UUID tourId) {
        return ResponseEntity.ok(tourService.getTourById(tourId));
    }

    @GetMapping("/my-tours")
    public ResponseEntity<List<TourResponse>> getMyTours() {
        return ResponseEntity.ok(tourService.getToursByUserId(null));
    }

    @PutMapping("/{tourId}")
    public ResponseEntity<TourResponse> updateTour(@PathVariable UUID tourId, @RequestBody TourRequest request) {
        return ResponseEntity.ok(tourService.updateTour(tourId, request));
    }

    @DeleteMapping("/{tourId}")
    public ResponseEntity<Void> deleteTour(@PathVariable UUID tourId) {
        tourService.deleteTour(tourId);
        return ResponseEntity.noContent().build();
    }
}
