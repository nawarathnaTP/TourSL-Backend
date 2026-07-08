package com.tourplanner.planning.tour.service;

import com.tourplanner.planning.auth.entity.Role;
import com.tourplanner.planning.auth.entity.User;
import com.tourplanner.planning.config.TourAccessValidator;
import com.tourplanner.planning.tour.dto.DayResponse;
import com.tourplanner.planning.tour.dto.TourRequest;
import com.tourplanner.planning.tour.dto.TourResponse;
import com.tourplanner.planning.tour.entity.Day;
import com.tourplanner.planning.tour.entity.GuideTourPackage;
import com.tourplanner.planning.tour.entity.Tour;
import com.tourplanner.planning.tour.entity.TourType;
import com.tourplanner.planning.tour.repository.GuideTourPackageRepository;
import com.tourplanner.planning.tour.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourServiceImpl implements TourService {

    private final TourRepository tourRepository;
    private final GuideTourPackageRepository guideTourPackageRepository;
    private final TourAccessValidator accessValidator;

    @Override
    @Transactional
    public TourResponse createTour(TourRequest request) {
        if (request.getStartDay().isAfter(request.getEndDay())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        User user = accessValidator.getAuthenticatedUser();

        if (tourRepository.existsOverlappingTour(user.getId(), request.getStartDay(), request.getEndDay())) {
            throw new IllegalArgumentException("Tour dates overlap with an existing tour");
        }

        TourType tourType = user.getRole() == Role.GUIDE ? TourType.GUIDE : TourType.TOURIST;

        Tour tour = Tour.builder()
                .user(user)
                .title(request.getTitle())
                .startDay(request.getStartDay())
                .endDay(request.getEndDay())
                .tourType(tourType)
                .build();

        long duration = ChronoUnit.DAYS.between(request.getStartDay(), request.getEndDay()) + 1;

        List<Day> days = new ArrayList<>();
        for (int i = 0; i < duration; i++) {
            Day day = Day.builder()
                    .tour(tour)
                    .dayNo(i + 1)
                    .date(request.getStartDay().plusDays(i))
                    .build();
            days.add(day);
        }

        tour.setDays(days);

        Tour savedTour = tourRepository.save(tour);

        if (tourType == TourType.GUIDE) {
            GuideTourPackage guideTourPackage = GuideTourPackage.builder()
                    .tour(savedTour)
                    .build();
            guideTourPackageRepository.save(guideTourPackage);
        }

        return mapToResponse(savedTour);
    }

    @Override
    @Transactional(readOnly = true)
    public TourResponse getTourById(UUID tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));
        return mapToResponse(tour);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourResponse> getToursByUserId(UUID userId) {
        User user = accessValidator.getAuthenticatedUser();
        return tourRepository.findByUser_Id(user.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public TourResponse updateTour(UUID tourId, TourRequest request) {
        if (request.getStartDay().isAfter(request.getEndDay())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));

        accessValidator.verifyOwnershipAndModifiable(tour);

        if (tourRepository.existsOverlappingTourExcluding(tour.getUser().getId(), tourId, request.getStartDay(), request.getEndDay())) {
            throw new IllegalArgumentException("Tour dates overlap with an existing tour");
        }

        LocalDate oldStart = tour.getStartDay();
        LocalDate oldEnd = tour.getEndDay();
        LocalDate newStart = request.getStartDay();
        LocalDate newEnd = request.getEndDay();

        tour.setTitle(request.getTitle());
        tour.setStartDay(newStart);
        tour.setEndDay(newEnd);

        if (!oldStart.equals(newStart) || !oldEnd.equals(newEnd)) {
            tour.getDays().clear();

            long duration = ChronoUnit.DAYS.between(newStart, newEnd) + 1;
            for (int i = 0; i < duration; i++) {
                Day day = Day.builder()
                        .tour(tour)
                        .dayNo(i + 1)
                        .date(newStart.plusDays(i))
                        .build();
                tour.getDays().add(day);
            }
        }

        Tour savedTour = tourRepository.save(tour);
        return mapToResponse(savedTour);
    }

    @Override
    @Transactional
    public void deleteTour(UUID tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));

        accessValidator.verifyOwnershipAndModifiable(tour);

        tourRepository.delete(tour);
    }

    private TourResponse mapToResponse(Tour tour) {
        List<DayResponse> dayResponses = tour.getDays() != null
                ? tour.getDays().stream().map(this::mapDayToResponse).toList()
                : Collections.emptyList();

        return TourResponse.builder()
                .tourId(tour.getTourId())
                .userId(tour.getUser().getId())
                .title(tour.getTitle())
                .startDay(tour.getStartDay())
                .endDay(tour.getEndDay())
                .tourType(tour.getTourType().name())
                .createdAt(tour.getCreatedAt())
                .updatedAt(tour.getUpdatedAt())
                .days(dayResponses)
                .build();
    }

    private DayResponse mapDayToResponse(Day day) {
        return DayResponse.builder()
                .dayId(day.getDayId())
                .tourId(day.getTour().getTourId())
                .dayNo(day.getDayNo())
                .date(day.getDate())
                .lodgingId(day.getLodgingId())
                .stops(Collections.emptyList())
                .build();
    }
}
