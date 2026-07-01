package com.tourplanner.planning.stop.service;

import com.tourplanner.planning.location.entity.Location;
import com.tourplanner.planning.location.repository.LocationRepository;
import com.tourplanner.planning.stop.dto.ActivityResponse;
import com.tourplanner.planning.stop.dto.StopRequest;
import com.tourplanner.planning.stop.dto.StopResponse;
import com.tourplanner.planning.stop.entity.Stop;
import com.tourplanner.planning.stop.repository.StopRepository;
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
public class StopServiceImpl implements StopService {

    private final StopRepository stopRepository;
    private final DayRepository dayRepository;
    private final LocationRepository locationRepository;

    @Override
    @Transactional
    public StopResponse addStop(StopRequest request) {
        Day day = dayRepository.findById(request.getDayId())
                .orElseThrow(() -> new RuntimeException("Day not found with id: " + request.getDayId()));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + request.getLocationId()));

        Stop stop = Stop.builder()
                .day(day)
                .location(location)
                .stopOrder(request.getStopOrder())
                .duration(request.getDuration())
                .build();

        Stop savedStop = stopRepository.save(stop);
        return mapToResponse(savedStop);
    }

    @Override
    @Transactional(readOnly = true)
    public StopResponse getStopById(UUID stopId) {
        Stop stop = stopRepository.findById(stopId)
                .orElseThrow(() -> new RuntimeException("Stop not found with id: " + stopId));
        return mapToResponse(stop);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StopResponse> getStopsByDayId(UUID dayId) {
        return stopRepository.findByDay_DayIdOrderByStopOrder(dayId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public StopResponse updateStop(UUID stopId, StopRequest request) {
        Stop stop = stopRepository.findById(stopId)
                .orElseThrow(() -> new RuntimeException("Stop not found with id: " + stopId));

        if (request.getLocationId() != null) {
            Location location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new RuntimeException("Location not found with id: " + request.getLocationId()));
            stop.setLocation(location);
        }

        if (request.getStopOrder() != null) {
            stop.setStopOrder(request.getStopOrder());
        }

        if (request.getDuration() != null) {
            stop.setDuration(request.getDuration());
        }

        Stop savedStop = stopRepository.save(stop);
        return mapToResponse(savedStop);
    }

    @Override
    @Transactional
    public List<StopResponse> reorderStops(UUID dayId, List<UUID> stopIds) {
        List<Stop> stops = stopRepository.findByDay_DayIdOrderByStopOrder(dayId);

        if (stops.size() != stopIds.size()) {
            throw new RuntimeException("Provided stop IDs do not match the number of stops for day: " + dayId);
        }

        // Set all orders to negative values first to avoid unique constraint violations
        for (int i = 0; i < stops.size(); i++) {
            stops.get(i).setStopOrder(-(i + 1));
        }
        stopRepository.saveAllAndFlush(stops);

        // Now assign the correct order based on the provided stop IDs
        for (int i = 0; i < stopIds.size(); i++) {
            UUID stopId = stopIds.get(i);
            Stop stop = stops.stream()
                    .filter(s -> s.getStopId().equals(stopId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Stop not found with id: " + stopId));
            stop.setStopOrder(i + 1);
        }
        stopRepository.saveAllAndFlush(stops);

        return stopRepository.findByDay_DayIdOrderByStopOrder(dayId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public StopResponse moveStop(UUID stopId, UUID targetDayId, Integer targetOrder) {
        Stop stop = stopRepository.findById(stopId)
                .orElseThrow(() -> new RuntimeException("Stop not found with id: " + stopId));

        Day targetDay = dayRepository.findById(targetDayId)
                .orElseThrow(() -> new RuntimeException("Day not found with id: " + targetDayId));

        UUID sourceDayId = stop.getDay().getDayId();
        int oldOrder = stop.getStopOrder();

        // Remove from source day: shift stops above the removed position down by 1
        if (!sourceDayId.equals(targetDayId)) {
            List<Stop> sourceStops = stopRepository.findByDay_DayIdOrderByStopOrder(sourceDayId);
            for (Stop s : sourceStops) {
                if (s.getStopOrder() > oldOrder) {
                    s.setStopOrder(-(s.getStopOrder() - 1));
                }
            }
            // Temporarily set the moving stop's order to 0 to avoid conflicts
            stop.setStopOrder(0);
            stopRepository.saveAllAndFlush(sourceStops);
            // Flip negative orders to positive
            for (Stop s : sourceStops) {
                if (s.getStopOrder() < 0) {
                    s.setStopOrder(-s.getStopOrder());
                }
            }
            stopRepository.saveAllAndFlush(sourceStops);
        }

        // Insert into target day: shift stops at and above the target position up by 1
        List<Stop> targetStops = stopRepository.findByDay_DayIdOrderByStopOrder(targetDayId);

        // If same day, exclude the moving stop from the target list
        if (sourceDayId.equals(targetDayId)) {
            targetStops.removeIf(s -> s.getStopId().equals(stopId));

            // Temporarily clear the moving stop's order
            stop.setStopOrder(0);
            stopRepository.saveAndFlush(stop);

            // Compact the remaining orders
            for (int i = 0; i < targetStops.size(); i++) {
                targetStops.get(i).setStopOrder(-(i + 1));
            }
            stopRepository.saveAllAndFlush(targetStops);
            for (int i = 0; i < targetStops.size(); i++) {
                targetStops.get(i).setStopOrder(i + 1);
            }
            stopRepository.saveAllAndFlush(targetStops);

            // Reload for insertion
            targetStops = stopRepository.findByDay_DayIdOrderByStopOrder(targetDayId);
            targetStops.removeIf(s -> s.getStopId().equals(stopId));
        }

        // Shift stops at and above targetOrder up to make room
        for (Stop s : targetStops) {
            if (s.getStopOrder() >= targetOrder) {
                s.setStopOrder(-(s.getStopOrder() + 1));
            }
        }
        stopRepository.saveAllAndFlush(targetStops);
        for (Stop s : targetStops) {
            if (s.getStopOrder() < 0) {
                s.setStopOrder(-s.getStopOrder());
            }
        }
        stopRepository.saveAllAndFlush(targetStops);

        // Place the stop in its new position
        stop.setDay(targetDay);
        stop.setStopOrder(targetOrder);
        Stop savedStop = stopRepository.saveAndFlush(stop);

        return mapToResponse(savedStop);
    }

    @Override
    @Transactional
    public void deleteStop(UUID stopId) {
        Stop stop = stopRepository.findById(stopId)
                .orElseThrow(() -> new RuntimeException("Stop not found with id: " + stopId));
        stopRepository.delete(stop);
    }

    private StopResponse mapToResponse(Stop stop) {
        List<ActivityResponse> activityResponses = stop.getActivities() != null
                ? stop.getActivities().stream().map(activity -> ActivityResponse.builder()
                        .activityId(activity.getActivityId())
                        .stopId(stop.getStopId())
                        .duration(activity.getDuration())
                        .description(activity.getDescription())
                        .bookingId(activity.getBookingId())
                        .build())
                .toList()
                : Collections.emptyList();

        return StopResponse.builder()
                .stopId(stop.getStopId())
                .dayId(stop.getDay().getDayId())
                .locationId(stop.getLocation() != null ? stop.getLocation().getLocationId() : null)
                .stopOrder(stop.getStopOrder())
                .duration(stop.getDuration())
                .activities(activityResponses)
                .build();
    }
}
