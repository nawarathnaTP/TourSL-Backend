package com.tourplanner.planning.route.service;

import com.tourplanner.planning.route.dto.RouteRequest;
import com.tourplanner.planning.route.dto.RouteResponse;
import com.tourplanner.planning.route.entity.Route;
import com.tourplanner.planning.route.entity.TransportOption;
import com.tourplanner.planning.route.repository.RouteRepository;
import com.tourplanner.planning.route.repository.TransportOptionRepository;
import com.tourplanner.planning.stop.entity.Stop;
import com.tourplanner.planning.stop.repository.StopRepository;
import com.tourplanner.planning.tour.entity.Day;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteServiceImplTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private StopRepository stopRepository;

    @Mock
    private TransportOptionRepository transportOptionRepository;

    @InjectMocks
    private RouteServiceImpl routeService;

    private UUID routeId;
    private UUID startStopId;
    private UUID endStopId;
    private UUID transportId;
    private UUID dayId;
    private Stop startStop;
    private Stop endStop;
    private TransportOption transportOption;
    private Route testRoute;

    @BeforeEach
    void setUp() {
        routeId = UUID.randomUUID();
        startStopId = UUID.randomUUID();
        endStopId = UUID.randomUUID();
        transportId = UUID.randomUUID();
        dayId = UUID.randomUUID();

        Day day = Day.builder().dayId(dayId).dayNo(1).build();

        startStop = Stop.builder()
                .stopId(startStopId)
                .day(day)
                .stopOrder(1)
                .duration(60)
                .build();

        endStop = Stop.builder()
                .stopId(endStopId)
                .day(day)
                .stopOrder(2)
                .duration(60)
                .build();

        transportOption = TransportOption.builder()
                .transportId(transportId)
                .type("bus")
                .label("Public Bus")
                .build();

        testRoute = Route.builder()
                .routeId(routeId)
                .startStop(startStop)
                .endStop(endStop)
                .transportOption(transportOption)
                .distance(BigDecimal.valueOf(25.5))
                .time(45)
                .cost(BigDecimal.valueOf(150))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    // Verifies that a route is created with the correct stops, transport, and metrics
    @Test
    void createRoute_validRequest_returnsRouteResponse() {
        RouteRequest request = RouteRequest.builder()
                .startStopId(startStopId)
                .endStopId(endStopId)
                .transportType("bus")
                .transportLabel("Public Bus")
                .distance(BigDecimal.valueOf(25.5))
                .time(45)
                .cost(BigDecimal.valueOf(150))
                .build();

        when(stopRepository.findById(startStopId)).thenReturn(Optional.of(startStop));
        when(stopRepository.findById(endStopId)).thenReturn(Optional.of(endStop));
        when(routeRepository.findByStartStop_StopIdAndEndStop_StopId(startStopId, endStopId))
                .thenReturn(Optional.empty());
        when(transportOptionRepository.findByType("bus")).thenReturn(Optional.of(transportOption));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> {
            Route route = invocation.getArgument(0);
            route.setRouteId(routeId);
            route.setCreatedAt(OffsetDateTime.now());
            route.setUpdatedAt(OffsetDateTime.now());
            return route;
        });

        RouteResponse response = routeService.createRoute(request);

        assertThat(response.getRouteId()).isEqualTo(routeId);
        assertThat(response.getStartStopId()).isEqualTo(startStopId);
        assertThat(response.getEndStopId()).isEqualTo(endStopId);
        assertThat(response.getTransport().getType()).isEqualTo("bus");
        assertThat(response.getDistance()).isEqualByComparingTo(BigDecimal.valueOf(25.5));
        assertThat(response.getTime()).isEqualTo(45);
        assertThat(response.getCost()).isEqualByComparingTo(BigDecimal.valueOf(150));

        verify(routeRepository).save(any(Route.class));
    }

    // Verifies that creating a route with a non-existent start stop throws an exception
    @Test
    void createRoute_startStopNotFound_throwsException() {
        RouteRequest request = RouteRequest.builder()
                .startStopId(UUID.randomUUID())
                .endStopId(endStopId)
                .build();

        when(stopRepository.findById(request.getStartStopId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.createRoute(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Start stop not found with id:");
    }

    // Verifies that creating a route with a non-existent end stop throws an exception
    @Test
    void createRoute_endStopNotFound_throwsException() {
        RouteRequest request = RouteRequest.builder()
                .startStopId(startStopId)
                .endStopId(UUID.randomUUID())
                .build();

        when(stopRepository.findById(startStopId)).thenReturn(Optional.of(startStop));
        when(stopRepository.findById(request.getEndStopId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.createRoute(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("End stop not found with id:");
    }

    // Verifies that creating a duplicate route between the same stops throws an exception
    @Test
    void createRoute_duplicateRoute_throwsException() {
        RouteRequest request = RouteRequest.builder()
                .startStopId(startStopId)
                .endStopId(endStopId)
                .build();

        when(stopRepository.findById(startStopId)).thenReturn(Optional.of(startStop));
        when(stopRepository.findById(endStopId)).thenReturn(Optional.of(endStop));
        when(routeRepository.findByStartStop_StopIdAndEndStop_StopId(startStopId, endStopId))
                .thenReturn(Optional.of(testRoute));

        assertThatThrownBy(() -> routeService.createRoute(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Route already exists between these stops");
    }

    // Verifies that creating a route with a new transport type creates the transport option
    @Test
    void createRoute_newTransportType_createsTransportOption() {
        RouteRequest request = RouteRequest.builder()
                .startStopId(startStopId)
                .endStopId(endStopId)
                .transportType("train")
                .transportLabel("Express Train")
                .distance(BigDecimal.valueOf(50))
                .time(90)
                .cost(BigDecimal.valueOf(500))
                .build();

        TransportOption newTransport = TransportOption.builder()
                .transportId(UUID.randomUUID())
                .type("train")
                .label("Express Train")
                .build();

        when(stopRepository.findById(startStopId)).thenReturn(Optional.of(startStop));
        when(stopRepository.findById(endStopId)).thenReturn(Optional.of(endStop));
        when(routeRepository.findByStartStop_StopIdAndEndStop_StopId(startStopId, endStopId))
                .thenReturn(Optional.empty());
        when(transportOptionRepository.findByType("train")).thenReturn(Optional.empty());
        when(transportOptionRepository.save(any(TransportOption.class))).thenReturn(newTransport);
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> {
            Route route = invocation.getArgument(0);
            route.setRouteId(routeId);
            route.setCreatedAt(OffsetDateTime.now());
            route.setUpdatedAt(OffsetDateTime.now());
            return route;
        });

        RouteResponse response = routeService.createRoute(request);

        assertThat(response.getTransport().getType()).isEqualTo("train");
        verify(transportOptionRepository).save(any(TransportOption.class));
    }

    // Verifies that fetching a route by ID returns the correct response
    @Test
    void getRouteById_existingRoute_returnsRouteResponse() {
        when(routeRepository.findById(routeId)).thenReturn(Optional.of(testRoute));

        RouteResponse response = routeService.getRouteById(routeId);

        assertThat(response.getRouteId()).isEqualTo(routeId);
        assertThat(response.getStartStopId()).isEqualTo(startStopId);
        assertThat(response.getEndStopId()).isEqualTo(endStopId);
        assertThat(response.getTransport().getTransportId()).isEqualTo(transportId);
    }

    // Verifies that fetching a non-existent route throws a RuntimeException
    @Test
    void getRouteById_nonExistingRoute_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(routeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.getRouteById(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Route not found with id:");
    }

    // Verifies that fetching routes by day ID returns all routes for that day
    @Test
    void getRoutesByDayId_returnsRouteList() {
        when(routeRepository.findByDayId(dayId)).thenReturn(List.of(testRoute));

        List<RouteResponse> responses = routeService.getRoutesByDayId(dayId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getRouteId()).isEqualTo(routeId);
    }

    // Verifies that fetching routes for a day with no routes returns an empty list
    @Test
    void getRoutesByDayId_noRoutes_returnsEmptyList() {
        when(routeRepository.findByDayId(dayId)).thenReturn(Collections.emptyList());

        List<RouteResponse> responses = routeService.getRoutesByDayId(dayId);

        assertThat(responses).isEmpty();
    }

    // Verifies that updating a route's distance only changes the distance
    @Test
    void updateRoute_distanceOnly_updatesDistance() {
        RouteRequest request = RouteRequest.builder()
                .distance(BigDecimal.valueOf(30.0))
                .build();

        when(routeRepository.findById(routeId)).thenReturn(Optional.of(testRoute));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RouteResponse response = routeService.updateRoute(routeId, request);

        assertThat(response.getDistance()).isEqualByComparingTo(BigDecimal.valueOf(30.0));
        assertThat(response.getTime()).isEqualTo(45);
        assertThat(response.getCost()).isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    // Verifies that updating a route's transport type changes the transport reference
    @Test
    void updateRoute_transportType_updatesTransport() {
        TransportOption newTransport = TransportOption.builder()
                .transportId(UUID.randomUUID())
                .type("taxi")
                .label("Private Taxi")
                .build();

        RouteRequest request = RouteRequest.builder()
                .transportType("taxi")
                .transportLabel("Private Taxi")
                .build();

        when(routeRepository.findById(routeId)).thenReturn(Optional.of(testRoute));
        when(transportOptionRepository.findByType("taxi")).thenReturn(Optional.of(newTransport));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RouteResponse response = routeService.updateRoute(routeId, request);

        assertThat(response.getTransport().getType()).isEqualTo("taxi");
    }

    // Verifies that updating a non-existent route throws an exception
    @Test
    void updateRoute_routeNotFound_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        RouteRequest request = RouteRequest.builder().time(60).build();

        when(routeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.updateRoute(nonExistentId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Route not found with id:");
    }

    // Verifies that an existing route is deleted via the repository
    @Test
    void deleteRoute_existingRoute_deletesSuccessfully() {
        when(routeRepository.findById(routeId)).thenReturn(Optional.of(testRoute));

        routeService.deleteRoute(routeId);

        verify(routeRepository).delete(testRoute);
    }

    // Verifies that deleting a non-existent route throws a RuntimeException
    @Test
    void deleteRoute_nonExistingRoute_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(routeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.deleteRoute(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Route not found with id:");
    }

    // Verifies that deleteRoutesForDay deletes all routes for the given day
    @Test
    void deleteRoutesForDay_deletesAllRoutes() {
        when(routeRepository.findByDayId(dayId)).thenReturn(List.of(testRoute));

        routeService.deleteRoutesForDay(dayId);

        verify(routeRepository).deleteAll(List.of(testRoute));
    }

    // Verifies that deleteRoutesForDay with no routes does not throw
    @Test
    void deleteRoutesForDay_noRoutes_deletesEmptyList() {
        when(routeRepository.findByDayId(dayId)).thenReturn(Collections.emptyList());

        routeService.deleteRoutesForDay(dayId);

        verify(routeRepository).deleteAll(Collections.emptyList());
    }

    // Verifies that a route with null transport option returns null transport in response
    @Test
    void getRouteById_nullTransport_returnsNullTransportInResponse() {
        testRoute.setTransportOption(null);
        when(routeRepository.findById(routeId)).thenReturn(Optional.of(testRoute));

        RouteResponse response = routeService.getRouteById(routeId);

        assertThat(response.getTransport()).isNull();
    }
}
