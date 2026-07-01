package com.tourplanner.planning.route.service;

import com.tourplanner.planning.route.dto.RouteRequest;
import com.tourplanner.planning.route.dto.RouteResponse;

import java.util.List;
import java.util.UUID;

public interface RouteService {

    RouteResponse createRoute(RouteRequest request);

    RouteResponse getRouteById(UUID routeId);

    List<RouteResponse> getRoutesByDayId(UUID dayId);

    RouteResponse updateRoute(UUID routeId, RouteRequest request);

    void deleteRoute(UUID routeId);

    void deleteRoutesForDay(UUID dayId);
}
