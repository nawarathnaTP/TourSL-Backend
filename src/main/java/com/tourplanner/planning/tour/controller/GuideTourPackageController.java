package com.tourplanner.planning.tour.controller;

import com.tourplanner.planning.tour.dto.GuideTourPackageRequest;
import com.tourplanner.planning.tour.dto.GuideTourPackageResponse;
import com.tourplanner.planning.tour.service.GuideTourPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/guide-packages")
@RequiredArgsConstructor
public class GuideTourPackageController {

    private final GuideTourPackageService guideTourPackageService;

    @GetMapping("/tour/{tourId}")
    public ResponseEntity<GuideTourPackageResponse> getPackageByTourId(@PathVariable UUID tourId) {
        return ResponseEntity.ok(guideTourPackageService.getPackageByTourId(tourId));
    }

    @PutMapping("/tour/{tourId}")
    public ResponseEntity<GuideTourPackageResponse> updatePackage(
            @PathVariable UUID tourId,
            @RequestBody GuideTourPackageRequest request) {
        return ResponseEntity.ok(guideTourPackageService.updatePackage(tourId, request));
    }

    @PatchMapping("/tour/{tourId}/publish")
    public ResponseEntity<GuideTourPackageResponse> publishPackage(@PathVariable UUID tourId) {
        return ResponseEntity.ok(guideTourPackageService.publishPackage(tourId));
    }

    @PatchMapping("/tour/{tourId}/cancel")
    public ResponseEntity<GuideTourPackageResponse> cancelPackage(@PathVariable UUID tourId) {
        return ResponseEntity.ok(guideTourPackageService.cancelPackage(tourId));
    }

    @GetMapping("/my-packages")
    public ResponseEntity<List<GuideTourPackageResponse>> getMyPackages() {
        return ResponseEntity.ok(guideTourPackageService.getMyPackages());
    }

    @GetMapping("/published")
    public ResponseEntity<List<GuideTourPackageResponse>> getPublishedPackages() {
        return ResponseEntity.ok(guideTourPackageService.getPublishedPackages());
    }
}
