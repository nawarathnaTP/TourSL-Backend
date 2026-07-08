package com.tourplanner.planning.tour.service;

import com.tourplanner.planning.config.TourAccessValidator;
import com.tourplanner.planning.stop.dto.StopResponse;
import com.tourplanner.planning.tour.dto.DayRequest;
import com.tourplanner.planning.tour.dto.DayResponse;
import com.tourplanner.planning.tour.entity.Day;
import com.tourplanner.planning.tour.repository.DayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DayServiceImpl implements DayService {

    private final DayRepository dayRepository;
    private final TourAccessValidator accessValidator;

    @Override
    @Transactional(readOnly = true)
    public DayResponse getDayById(UUID dayId) {
        Day day = dayRepository.findById(dayId)
                .orElseThrow(() -> new RuntimeException("Day not found with id: " + dayId));
        return mapToResponse(day);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DayResponse> getDaysByTourId(UUID tourId) {
        return dayRepository.findByTour_TourIdOrderByDayNo(tourId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public DayResponse updateDay(UUID dayId, DayRequest request) {
        Day day = dayRepository.findById(dayId)
                .orElseThrow(() -> new RuntimeException("Day not found with id: " + dayId));

        accessValidator.verifyOwnershipAndModifiable(day.getTour());

        if (request.getLodgingId() != null) {
            day.setLodgingId(request.getLodgingId());
        }

        Day savedDay = dayRepository.save(day);
        return mapToResponse(savedDay);
    }

    @Override
    @Transactional
    public DayResponse clearDay(UUID dayId) {
        Day day = dayRepository.findById(dayId)
                .orElseThrow(() -> new RuntimeException("Day not found with id: " + dayId));

        accessValidator.verifyOwnershipAndModifiable(day.getTour());

        day.getStops().clear();
        day.setLodgingId(null);

        Day savedDay = dayRepository.save(day);
        return mapToResponse(savedDay);
    }

    private DayResponse mapToResponse(Day day) {
        List<StopResponse> stopResponses = day.getStops() != null
                ? day.getStops().stream().map(stop -> StopResponse.builder()
                        .stopId(stop.getStopId())
                        .dayId(day.getDayId())
                        .locationId(stop.getLocation() != null ? stop.getLocation().getLocationId() : null)
                        .stopOrder(stop.getStopOrder())
                        .duration(stop.getDuration())
                        .activities(Collections.emptyList())
                        .build())
                .toList()
                : Collections.emptyList();

        return DayResponse.builder()
                .dayId(day.getDayId())
                .tourId(day.getTour().getTourId())
                .dayNo(day.getDayNo())
                .date(day.getDate())
                .lodgingId(day.getLodgingId())
                .stops(stopResponses)
                .build();
    }
}
