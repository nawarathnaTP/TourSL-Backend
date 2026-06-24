package com.tourplanner.planning.route.service;

import com.tourplanner.planning.route.dto.RouteOptionResponse;
import com.tourplanner.planning.route.dto.RouteRequest;
import com.tourplanner.planning.route.dto.RouteResponse;

import java.util.List;
import java.util.UUID;

public interface RouteService {

    RouteResponse createRoute(RouteRequest request);

    RouteResponse getRouteById(UUID routeId);

    List<RouteResponse> getRoutesByStopId(UUID stopId);

    void deleteRoute(UUID routeId);

    List<RouteOptionResponse> getRouteOptionsByRouteId(UUID routeId);

    RouteOptionResponse selectRouteOption(UUID optionId);
}
