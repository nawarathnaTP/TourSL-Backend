package com.tourplanner.planning.tour.service;

import com.tourplanner.planning.tour.dto.TourRequest;
import com.tourplanner.planning.tour.dto.TourResponse;

import java.util.List;
import java.util.UUID;

public interface TourService {

    TourResponse createTour(TourRequest request);

    TourResponse getTourById(UUID tourId);

    List<TourResponse> getToursByUserId(Long userId);

    TourResponse updateTour(UUID tourId, TourRequest request);

    void deleteTour(UUID tourId);
}
