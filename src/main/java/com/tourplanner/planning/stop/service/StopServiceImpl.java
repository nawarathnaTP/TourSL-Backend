package com.tourplanner.planning.stop.service;

import com.tourplanner.planning.config.TourAccessValidator;
import com.tourplanner.planning.location.entity.Location;
import com.tourplanner.planning.location.service.LocationService;
import com.tourplanner.planning.route.entity.Route;
import com.tourplanner.planning.route.repository.RouteRepository;
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
    private final LocationService locationService;
    private final RouteRepository routeRepository;
    private final TourAccessValidator accessValidator;

    @Override
    @Transactional
    public StopResponse addStop(StopRequest request) {
        Day day = dayRepository.findById(request.getDayId())
                .orElseThrow(() -> new RuntimeException("Day not found with id: " + request.getDayId()));

        accessValidator.verifyOwnershipAndModifiable(day.getTour());

        Location location = locationService.findOrCreate(request.getLocation());

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

        accessValidator.verifyOwnershipAndModifiable(stop.getDay().getTour());

        if (request.getLocation() != null) {
            Location location = locationService.findOrCreate(request.getLocation());
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

        if (!stops.isEmpty()) {
            accessValidator.verifyOwnershipAndModifiable(stops.get(0).getDay().getTour());
        }

        if (stops.size() != stopIds.size()) {
            throw new RuntimeException("Provided stop IDs do not match the number of stops for day: " + dayId);
        }

        // Capture old consecutive pairs before reordering
        List<UUID> oldOrder = stops.stream().map(Stop::getStopId).toList();

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

        // Invalidate routes for changed consecutive pairs
        invalidateChangedRoutes(oldOrder, stopIds);

        return stopRepository.findByDay_DayIdOrderByStopOrder(dayId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public StopResponse moveStop(UUID stopId, UUID targetDayId, Integer targetOrder) {
        Stop stop = stopRepository.findById(stopId)
                .orElseThrow(() -> new RuntimeException("Stop not found with id: " + stopId));

        accessValidator.verifyOwnershipAndModifiable(stop.getDay().getTour());

        Day targetDay = dayRepository.findById(targetDayId)
                .orElseThrow(() -> new RuntimeException("Day not found with id: " + targetDayId));

        // Also verify target day belongs to the same tour
        accessValidator.verifyOwnershipAndModifiable(targetDay.getTour());

        UUID sourceDayId = stop.getDay().getDayId();
        int oldOrder = stop.getStopOrder();

        // Capture old consecutive pairs for both source and target days before moving
        List<UUID> oldSourceOrder = stopRepository.findByDay_DayIdOrderByStopOrder(sourceDayId)
                .stream().map(Stop::getStopId).toList();
        List<UUID> oldTargetOrder = sourceDayId.equals(targetDayId)
                ? oldSourceOrder
                : stopRepository.findByDay_DayIdOrderByStopOrder(targetDayId)
                        .stream().map(Stop::getStopId).toList();

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

        // Invalidate routes for changed consecutive pairs in both days
        List<UUID> newSourceOrder = stopRepository.findByDay_DayIdOrderByStopOrder(sourceDayId)
                .stream().map(Stop::getStopId).toList();
        invalidateChangedRoutes(oldSourceOrder, newSourceOrder);

        if (!sourceDayId.equals(targetDayId)) {
            List<UUID> newTargetOrder = stopRepository.findByDay_DayIdOrderByStopOrder(targetDayId)
                    .stream().map(Stop::getStopId).toList();
            invalidateChangedRoutes(oldTargetOrder, newTargetOrder);
        }

        return mapToResponse(savedStop);
    }

    @Override
    @Transactional
    public void deleteStop(UUID stopId) {
        Stop stop = stopRepository.findById(stopId)
                .orElseThrow(() -> new RuntimeException("Stop not found with id: " + stopId));

        accessValidator.verifyOwnershipAndModifiable(stop.getDay().getTour());

        // Delete all routes involving this stop
        List<Route> affectedRoutes = routeRepository.findByStopId(stopId);
        routeRepository.deleteAll(affectedRoutes);

        stopRepository.delete(stop);
    }

    /**
     * Compares old and new consecutive pairs, deletes routes for pairs that changed.
     */
    private void invalidateChangedRoutes(List<UUID> oldOrder, List<UUID> newOrder) {
        for (int i = 0; i < oldOrder.size() - 1; i++) {
            UUID oldStart = oldOrder.get(i);
            UUID oldEnd = oldOrder.get(i + 1);

            // Check if this pair still exists in the new order
            boolean pairPreserved = false;
            for (int j = 0; j < newOrder.size() - 1; j++) {
                if (newOrder.get(j).equals(oldStart) && newOrder.get(j + 1).equals(oldEnd)) {
                    pairPreserved = true;
                    break;
                }
            }

            if (!pairPreserved) {
                routeRepository.findByStartStop_StopIdAndEndStop_StopId(oldStart, oldEnd)
                        .ifPresent(routeRepository::delete);
            }
        }
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
