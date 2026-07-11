package com.tourplanner.planning.route.service;

import com.tourplanner.planning.config.TourAccessValidator;
import com.tourplanner.planning.route.dto.RouteRequest;
import com.tourplanner.planning.route.dto.RouteResponse;
import com.tourplanner.planning.route.dto.TransportOptionResponse;
import com.tourplanner.planning.route.entity.Route;
import com.tourplanner.planning.route.entity.TransportOption;
import com.tourplanner.planning.route.repository.RouteRepository;
import com.tourplanner.planning.route.repository.TransportOptionRepository;
import com.tourplanner.planning.stop.entity.Stop;
import com.tourplanner.planning.stop.repository.StopRepository;
import com.tourplanner.planning.tour.entity.Day;
import com.tourplanner.planning.tour.repository.DayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final TransportOptionRepository transportOptionRepository;
    private final DayRepository dayRepository;
    private final TourAccessValidator accessValidator;

    @Override
    @Transactional
    public RouteResponse createRoute(RouteRequest request) {
        Stop startStop = stopRepository.findById(request.getStartStopId())
                .orElseThrow(() -> new RuntimeException("Start stop not found with id: " + request.getStartStopId()));

        accessValidator.verifyOwnershipAndModifiable(startStop.getDay().getTour());

        Stop endStop = stopRepository.findById(request.getEndStopId())
                .orElseThrow(() -> new RuntimeException("End stop not found with id: " + request.getEndStopId()));

        // Check if a route already exists for this pair
        routeRepository.findByStartStop_StopIdAndEndStop_StopId(
                request.getStartStopId(), request.getEndStopId())
                .ifPresent(existing -> {
                    throw new RuntimeException("Route already exists between these stops with id: " + existing.getRouteId());
                });

        TransportOption transport = findOrCreateTransport(request.getTransportType(), request.getTransportLabel());

        Route route = Route.builder()
                .startStop(startStop)
                .endStop(endStop)
                .transportOption(transport)
                .distance(request.getDistance())
                .time(request.getTime())
                .cost(request.getCost())
                .polyline(request.getPolyline())
                .build();

        Route saved = routeRepository.save(route);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponse getRouteById(UUID routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Route not found with id: " + routeId));
        return mapToResponse(route);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteResponse> getRoutesByDayId(UUID dayId) {
        return routeRepository.findByDayId(dayId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public RouteResponse updateRoute(UUID routeId, RouteRequest request) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Route not found with id: " + routeId));

        accessValidator.verifyOwnershipAndModifiable(route.getStartStop().getDay().getTour());

        if (request.getTransportType() != null) {
            TransportOption transport = findOrCreateTransport(request.getTransportType(), request.getTransportLabel());
            route.setTransportOption(transport);
        }

        if (request.getDistance() != null) {
            route.setDistance(request.getDistance());
        }

        if (request.getTime() != null) {
            route.setTime(request.getTime());
        }

        if (request.getCost() != null) {
            route.setCost(request.getCost());
        }

        if (request.getPolyline() != null) {
            route.setPolyline(request.getPolyline());
        }

        Route saved = routeRepository.save(route);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRoute(UUID routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Route not found with id: " + routeId));

        accessValidator.verifyOwnershipAndModifiable(route.getStartStop().getDay().getTour());

        routeRepository.delete(route);
    }

    @Override
    @Transactional
    public void deleteRoutesForDay(UUID dayId) {
        Day day = dayRepository.findById(dayId)
                .orElseThrow(() -> new RuntimeException("Day not found with id: " + dayId));

        accessValidator.verifyOwnershipAndModifiable(day.getTour());

        List<Route> routes = routeRepository.findByDayId(dayId);
        routeRepository.deleteAll(routes);
    }

    private TransportOption findOrCreateTransport(String type, String label) {
        return transportOptionRepository.findByType(type)
                .orElseGet(() -> transportOptionRepository.save(TransportOption.builder()
                        .type(type)
                        .label(label)
                        .build()));
    }

    private RouteResponse mapToResponse(Route route) {
        TransportOptionResponse transportResponse = null;
        if (route.getTransportOption() != null) {
            transportResponse = TransportOptionResponse.builder()
                    .transportId(route.getTransportOption().getTransportId())
                    .type(route.getTransportOption().getType())
                    .label(route.getTransportOption().getLabel())
                    .build();
        }

        return RouteResponse.builder()
                .routeId(route.getRouteId())
                .startStopId(route.getStartStop().getStopId())
                .endStopId(route.getEndStop().getStopId())
                .transport(transportResponse)
                .distance(route.getDistance())
                .time(route.getTime())
                .cost(route.getCost())
                .polyline(route.getPolyline())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
    }
}
