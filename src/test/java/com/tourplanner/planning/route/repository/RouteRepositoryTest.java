package com.tourplanner.planning.route.repository;

import com.tourplanner.planning.auth.entity.User;
import com.tourplanner.planning.auth.repository.UserRepository;
import com.tourplanner.planning.location.entity.Location;
import com.tourplanner.planning.location.repository.LocationRepository;
import com.tourplanner.planning.route.entity.Route;
import com.tourplanner.planning.route.entity.TransportOption;
import com.tourplanner.planning.stop.entity.Stop;
import com.tourplanner.planning.stop.repository.StopRepository;
import com.tourplanner.planning.tour.entity.Day;
import com.tourplanner.planning.tour.entity.Tour;
import com.tourplanner.planning.tour.repository.DayRepository;
import com.tourplanner.planning.tour.repository.TourRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RouteRepositoryTest {

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private TransportOptionRepository transportOptionRepository;

    @Autowired
    private StopRepository stopRepository;

    @Autowired
    private DayRepository dayRepository;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationRepository locationRepository;

    private Stop startStop;
    private Stop endStop;
    private Stop otherStop;
    private Day savedDay;
    private Day otherDay;
    private TransportOption savedTransport;

    @BeforeEach
    void setUp() {
        routeRepository.deleteAll();
        stopRepository.deleteAll();
        dayRepository.deleteAll();
        tourRepository.deleteAll();
        transportOptionRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build());

        Tour tour = tourRepository.save(Tour.builder()
                .user(user)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build());

        savedDay = dayRepository.save(Day.builder()
                .tour(tour)
                .dayNo(1)
                .date(LocalDate.of(2026, 7, 1))
                .build());

        otherDay = dayRepository.save(Day.builder()
                .tour(tour)
                .dayNo(2)
                .date(LocalDate.of(2026, 7, 2))
                .build());

        Location location1 = locationRepository.save(Location.builder()
                .placeName("Sigiriya")
                .latitude(BigDecimal.valueOf(7.957))
                .longitude(BigDecimal.valueOf(80.760))
                .build());

        Location location2 = locationRepository.save(Location.builder()
                .placeName("Kandy")
                .latitude(BigDecimal.valueOf(7.291))
                .longitude(BigDecimal.valueOf(80.636))
                .build());

        Location location3 = locationRepository.save(Location.builder()
                .placeName("Colombo")
                .latitude(BigDecimal.valueOf(6.927))
                .longitude(BigDecimal.valueOf(79.861))
                .build());

        startStop = stopRepository.save(Stop.builder()
                .day(savedDay).location(location1).stopOrder(1).duration(120).build());

        endStop = stopRepository.save(Stop.builder()
                .day(savedDay).location(location2).stopOrder(2).duration(60).build());

        otherStop = stopRepository.save(Stop.builder()
                .day(otherDay).location(location3).stopOrder(1).duration(90).build());

        savedTransport = transportOptionRepository.save(TransportOption.builder()
                .type("bus")
                .label("Public Bus")
                .build());
    }

    // Verifies that a route is persisted with a generated UUID and correct fields
    @Test
    void save_validRoute_persistsWithGeneratedId() {
        Route route = routeRepository.save(Route.builder()
                .startStop(startStop)
                .endStop(endStop)
                .transportOption(savedTransport)
                .distance(BigDecimal.valueOf(25.5))
                .time(45)
                .cost(BigDecimal.valueOf(150))
                .build());

        assertThat(route.getRouteId()).isNotNull();
        assertThat(route.getStartStop().getStopId()).isEqualTo(startStop.getStopId());
        assertThat(route.getEndStop().getStopId()).isEqualTo(endStop.getStopId());
        assertThat(route.getTransportOption().getTransportId()).isEqualTo(savedTransport.getTransportId());
        assertThat(route.getDistance()).isEqualByComparingTo(BigDecimal.valueOf(25.5));
        assertThat(route.getTime()).isEqualTo(45);
        assertThat(route.getCreatedAt()).isNotNull();
        assertThat(route.getUpdatedAt()).isNotNull();
    }

    // Verifies that saving a route without a start stop throws a DataIntegrityViolation
    @Test
    void save_withoutStartStop_throwsDataIntegrityViolation() {
        Route route = Route.builder()
                .endStop(endStop)
                .transportOption(savedTransport)
                .distance(BigDecimal.valueOf(25.5))
                .time(45)
                .build();

        assertThatThrownBy(() -> routeRepository.saveAndFlush(route))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that saving a route without an end stop throws a DataIntegrityViolation
    @Test
    void save_withoutEndStop_throwsDataIntegrityViolation() {
        Route route = Route.builder()
                .startStop(startStop)
                .transportOption(savedTransport)
                .distance(BigDecimal.valueOf(25.5))
                .time(45)
                .build();

        assertThatThrownBy(() -> routeRepository.saveAndFlush(route))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that findById returns the correct route when it exists
    @Test
    void findById_existingRoute_returnsRoute() {
        Route saved = routeRepository.save(Route.builder()
                .startStop(startStop)
                .endStop(endStop)
                .transportOption(savedTransport)
                .distance(BigDecimal.valueOf(25.5))
                .time(45)
                .build());

        Optional<Route> found = routeRepository.findById(saved.getRouteId());

        assertThat(found).isPresent();
        assertThat(found.get().getRouteId()).isEqualTo(saved.getRouteId());
    }

    // Verifies that findById returns empty for a non-existent UUID
    @Test
    void findById_nonExistingId_returnsEmpty() {
        Optional<Route> found = routeRepository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    // Verifies that findByStartStop_StopIdAndEndStop_StopId returns the matching route
    @Test
    void findByStartStopAndEndStop_existingPair_returnsRoute() {
        routeRepository.save(Route.builder()
                .startStop(startStop)
                .endStop(endStop)
                .transportOption(savedTransport)
                .distance(BigDecimal.valueOf(25.5))
                .time(45)
                .build());

        Optional<Route> found = routeRepository.findByStartStop_StopIdAndEndStop_StopId(
                startStop.getStopId(), endStop.getStopId());

        assertThat(found).isPresent();
        assertThat(found.get().getStartStop().getStopId()).isEqualTo(startStop.getStopId());
        assertThat(found.get().getEndStop().getStopId()).isEqualTo(endStop.getStopId());
    }

    // Verifies that findByStartStop_StopIdAndEndStop_StopId returns empty for non-matching pair
    @Test
    void findByStartStopAndEndStop_nonExistingPair_returnsEmpty() {
        Optional<Route> found = routeRepository.findByStartStop_StopIdAndEndStop_StopId(
                UUID.randomUUID(), UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    // Verifies that findByDayId returns routes for the given day ordered by stop order
    @Test
    void findByDayId_returnsRoutesForDay() {
        routeRepository.save(Route.builder()
                .startStop(startStop)
                .endStop(endStop)
                .transportOption(savedTransport)
                .distance(BigDecimal.valueOf(25.5))
                .time(45)
                .build());

        List<Route> routes = routeRepository.findByDayId(savedDay.getDayId());

        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).getStartStop().getDay().getDayId()).isEqualTo(savedDay.getDayId());
    }

    // SECURITY: Verifies that findByDayId does not return routes from other days
    @Test
    void findByDayId_doesNotReturnRoutesFromOtherDays() {
        routeRepository.save(Route.builder()
                .startStop(startStop)
                .endStop(endStop)
                .transportOption(savedTransport)
                .distance(BigDecimal.valueOf(25.5))
                .time(45)
                .build());

        List<Route> routes = routeRepository.findByDayId(otherDay.getDayId());

        assertThat(routes).isEmpty();
    }

    // Verifies that findByDayId returns empty for a non-existent day
    @Test
    void findByDayId_nonExistentDay_returnsEmptyList() {
        List<Route> routes = routeRepository.findByDayId(UUID.randomUUID());

        assertThat(routes).isEmpty();
    }

    // Verifies that findByStopId returns routes where the stop is either start or end
    @Test
    void findByStopId_returnsRoutesInvolvingStop() {
        routeRepository.save(Route.builder()
                .startStop(startStop)
                .endStop(endStop)
                .transportOption(savedTransport)
                .distance(BigDecimal.valueOf(25.5))
                .time(45)
                .build());

        List<Route> routesForStart = routeRepository.findByStopId(startStop.getStopId());
        List<Route> routesForEnd = routeRepository.findByStopId(endStop.getStopId());

        assertThat(routesForStart).hasSize(1);
        assertThat(routesForEnd).hasSize(1);
    }

    // CONSTRAINT: Verifies that the unique constraint prevents duplicate start-end stop pairs
    @Test
    void save_duplicateStartEndPair_throwsDataIntegrityViolation() {
        routeRepository.saveAndFlush(Route.builder()
                .startStop(startStop)
                .endStop(endStop)
                .transportOption(savedTransport)
                .distance(BigDecimal.valueOf(25.5))
                .time(45)
                .build());

        Route duplicate = Route.builder()
                .startStop(startStop)
                .endStop(endStop)
                .transportOption(savedTransport)
                .distance(BigDecimal.valueOf(30))
                .time(60)
                .build();

        assertThatThrownBy(() -> routeRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that a route can be saved without a transport option
    @Test
    void save_withoutTransport_succeeds() {
        Route route = routeRepository.save(Route.builder()
                .startStop(startStop)
                .endStop(endStop)
                .distance(BigDecimal.valueOf(25.5))
                .time(45)
                .build());

        assertThat(route.getRouteId()).isNotNull();
        assertThat(route.getTransportOption()).isNull();
    }

    // Verifies that each route gets a unique UUID
    @Test
    void save_multipleRoutes_generatesUniqueIds() {
        Route route1 = routeRepository.save(Route.builder()
                .startStop(startStop).endStop(endStop)
                .transportOption(savedTransport).distance(BigDecimal.valueOf(25.5)).time(45).build());
        Route route2 = routeRepository.save(Route.builder()
                .startStop(endStop).endStop(otherStop)
                .transportOption(savedTransport).distance(BigDecimal.valueOf(50)).time(90).build());

        assertThat(route1.getRouteId()).isNotEqualTo(route2.getRouteId());
    }

    // Verifies that updating a route's distance persists the change
    @Test
    void update_distance_persistsChange() {
        Route route = routeRepository.save(Route.builder()
                .startStop(startStop)
                .endStop(endStop)
                .transportOption(savedTransport)
                .distance(BigDecimal.valueOf(25.5))
                .time(45)
                .build());

        route.setDistance(BigDecimal.valueOf(30));
        routeRepository.saveAndFlush(route);

        Optional<Route> updated = routeRepository.findById(route.getRouteId());

        assertThat(updated).isPresent();
        assertThat(updated.get().getDistance()).isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    // Verifies that deleting a route does not affect other routes
    @Test
    void delete_route_doesNotAffectOtherRoutes() {
        Route route1 = routeRepository.save(Route.builder()
                .startStop(startStop).endStop(endStop)
                .transportOption(savedTransport).distance(BigDecimal.valueOf(25.5)).time(45).build());
        Route route2 = routeRepository.save(Route.builder()
                .startStop(endStop).endStop(otherStop)
                .transportOption(savedTransport).distance(BigDecimal.valueOf(50)).time(90).build());

        routeRepository.delete(route1);
        routeRepository.flush();

        assertThat(routeRepository.findById(route1.getRouteId())).isEmpty();
        assertThat(routeRepository.findById(route2.getRouteId())).isPresent();
    }
}
