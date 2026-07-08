package com.tourplanner.planning.tour.service;

import com.tourplanner.planning.tour.dto.GuideTourPackageRequest;
import com.tourplanner.planning.tour.dto.GuideTourPackageResponse;

import java.util.List;
import java.util.UUID;

public interface GuideTourPackageService {

    GuideTourPackageResponse getPackageByTourId(UUID tourId);

    GuideTourPackageResponse updatePackage(UUID tourId, GuideTourPackageRequest request);

    GuideTourPackageResponse publishPackage(UUID tourId);

    GuideTourPackageResponse cancelPackage(UUID tourId);

    GuideTourPackageResponse unpublishPackage(UUID tourId);

    List<GuideTourPackageResponse> getMyPackages();

    List<GuideTourPackageResponse> getPublishedPackages();
}
