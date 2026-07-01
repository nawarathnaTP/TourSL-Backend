package com.tourplanner.planning.stop.repository;

import com.tourplanner.planning.auth.entity.User;
import com.tourplanner.planning.auth.repository.UserRepository;
import com.tourplanner.planning.location.entity.Location;
import com.tourplanner.planning.location.repository.LocationRepository;
import com.tourplanner.planning.stop.entity.Activity;
import com.tourplanner.planning.stop.entity.Stop;
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
class StopRepositoryTest {

    @Autowired
    private StopRepository stopRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private DayRepository dayRepository;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationRepository locationRepository;

    private Day savedDay;
    private Day otherDay;
    private Location savedLocation;
    private Location otherLocation;

    @BeforeEach
    void setUp() {
        activityRepository.deleteAll();
        stopRepository.deleteAll();
        dayRepository.deleteAll();
        tourRepository.deleteAll();
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

        savedLocation = locationRepository.save(Location.builder()
                .placeName("Sigiriya")
                .latitude(BigDecimal.valueOf(7.957))
                .longitude(BigDecimal.valueOf(80.760))
                .build());

        otherLocation = locationRepository.save(Location.builder()
                .placeName("Kandy")
                .latitude(BigDecimal.valueOf(7.291))
                .longitude(BigDecimal.valueOf(80.636))
                .build());
    }

    // Verifies that a stop is persisted with a generated UUID and correct fields
    @Test
    void save_validStop_persistsWithGeneratedId() {
        Stop stop = stopRepository.save(Stop.builder()
                .day(savedDay)
                .location(savedLocation)
                .stopOrder(1)
                .duration(120)
                .build());

        assertThat(stop.getStopId()).isNotNull();
        assertThat(stop.getDay().getDayId()).isEqualTo(savedDay.getDayId());
        assertThat(stop.getLocation().getLocationId()).isEqualTo(savedLocation.getLocationId());
        assertThat(stop.getStopOrder()).isEqualTo(1);
        assertThat(stop.getDuration()).isEqualTo(120);
    }

    // Verifies that saving a stop without a day reference violates the NOT NULL constraint
    @Test
    void save_withoutDay_throwsDataIntegrityViolation() {
        Stop stop = Stop.builder()
                .location(savedLocation)
                .stopOrder(1)
                .duration(60)
                .build();

        assertThatThrownBy(() -> stopRepository.saveAndFlush(stop))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that saving a stop without a location reference violates the NOT NULL constraint
    @Test
    void save_withoutLocation_throwsDataIntegrityViolation() {
        Stop stop = Stop.builder()
                .day(savedDay)
                .stopOrder(1)
                .duration(60)
                .build();

        assertThatThrownBy(() -> stopRepository.saveAndFlush(stop))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that saving a stop without a stop order violates the NOT NULL constraint
    @Test
    void save_withoutStopOrder_throwsDataIntegrityViolation() {
        Stop stop = Stop.builder()
                .day(savedDay)
                .location(savedLocation)
                .duration(60)
                .build();

        assertThatThrownBy(() -> stopRepository.saveAndFlush(stop))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that saving a stop without a duration violates the NOT NULL constraint
    @Test
    void save_withoutDuration_throwsDataIntegrityViolation() {
        Stop stop = Stop.builder()
                .day(savedDay)
                .location(savedLocation)
                .stopOrder(1)
                .build();

        assertThatThrownBy(() -> stopRepository.saveAndFlush(stop))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that findById returns the correct stop when it exists
    @Test
    void findById_existingStop_returnsStop() {
        Stop saved = stopRepository.save(Stop.builder()
                .day(savedDay)
                .location(savedLocation)
                .stopOrder(1)
                .duration(120)
                .build());

        Optional<Stop> found = stopRepository.findById(saved.getStopId());

        assertThat(found).isPresent();
        assertThat(found.get().getStopId()).isEqualTo(saved.getStopId());
    }

    // Verifies that findById returns empty for a non-existent UUID
    @Test
    void findById_nonExistingId_returnsEmpty() {
        Optional<Stop> found = stopRepository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    // Verifies that findByDay_DayIdOrderByStopOrder returns stops sorted by stop order
    @Test
    void findByDayId_returnsStopsOrderedByStopOrder() {
        stopRepository.save(Stop.builder()
                .day(savedDay).location(savedLocation).stopOrder(3).duration(45).build());
        stopRepository.save(Stop.builder()
                .day(savedDay).location(otherLocation).stopOrder(1).duration(120).build());
        stopRepository.save(Stop.builder()
                .day(savedDay).location(savedLocation).stopOrder(2).duration(60).build());

        List<Stop> stops = stopRepository.findByDay_DayIdOrderByStopOrder(savedDay.getDayId());

        assertThat(stops).hasSize(3);
        assertThat(stops.get(0).getStopOrder()).isEqualTo(1);
        assertThat(stops.get(1).getStopOrder()).isEqualTo(2);
        assertThat(stops.get(2).getStopOrder()).isEqualTo(3);
    }

    // SECURITY: Verifies that querying stops by day ID only returns stops for that specific day
    @Test
    void findByDayId_doesNotReturnStopsFromOtherDays() {
        stopRepository.save(Stop.builder()
                .day(savedDay).location(savedLocation).stopOrder(1).duration(120).build());
        stopRepository.save(Stop.builder()
                .day(savedDay).location(otherLocation).stopOrder(2).duration(60).build());
        stopRepository.save(Stop.builder()
                .day(otherDay).location(savedLocation).stopOrder(1).duration(90).build());

        List<Stop> day1Stops = stopRepository.findByDay_DayIdOrderByStopOrder(savedDay.getDayId());
        List<Stop> day2Stops = stopRepository.findByDay_DayIdOrderByStopOrder(otherDay.getDayId());

        assertThat(day1Stops).hasSize(2);
        assertThat(day2Stops).hasSize(1);
        assertThat(day1Stops).allMatch(s -> s.getDay().getDayId().equals(savedDay.getDayId()));
        assertThat(day2Stops).allMatch(s -> s.getDay().getDayId().equals(otherDay.getDayId()));
    }

    // SECURITY: Verifies that querying stops by a non-existent day ID returns an empty list
    @Test
    void findByDayId_nonExistentDay_returnsEmptyList() {
        stopRepository.save(Stop.builder()
                .day(savedDay).location(savedLocation).stopOrder(1).duration(120).build());

        List<Stop> stops = stopRepository.findByDay_DayIdOrderByStopOrder(UUID.randomUUID());

        assertThat(stops).isEmpty();
    }

    // CONSTRAINT: Verifies that the unique constraint (day_id, stop_order) prevents duplicate orders within the same day
    @Test
    void save_duplicateStopOrderSameDay_throwsDataIntegrityViolation() {
        stopRepository.saveAndFlush(Stop.builder()
                .day(savedDay).location(savedLocation).stopOrder(1).duration(120).build());

        Stop duplicate = Stop.builder()
                .day(savedDay).location(otherLocation).stopOrder(1).duration(60).build();

        assertThatThrownBy(() -> stopRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // CONSTRAINT: Verifies that the same stop order can be used in different days (unique per day, not global)
    @Test
    void save_sameStopOrderDifferentDays_succeeds() {
        Stop stop1 = stopRepository.save(Stop.builder()
                .day(savedDay).location(savedLocation).stopOrder(1).duration(120).build());
        Stop stop2 = stopRepository.save(Stop.builder()
                .day(otherDay).location(savedLocation).stopOrder(1).duration(90).build());

        assertThat(stop1.getStopId()).isNotNull();
        assertThat(stop2.getStopId()).isNotNull();
        assertThat(stop1.getStopOrder()).isEqualTo(stop2.getStopOrder());
    }

    // Verifies that deleting a stop cascades deletion to its activities
    @Test
    void delete_stop_cascadesDeleteToActivities() {
        Stop stop = stopRepository.save(Stop.builder()
                .day(savedDay).location(savedLocation).stopOrder(1).duration(120).build());

        Activity activity = activityRepository.save(Activity.builder()
                .stop(stop).duration(60).description("Climb the rock").build());

        UUID stopId = stop.getStopId();
        UUID activityId = activity.getActivityId();

        assertThat(activityRepository.findById(activityId)).isPresent();

        stopRepository.delete(stop);
        stopRepository.flush();

        assertThat(stopRepository.findById(stopId)).isEmpty();
        assertThat(activityRepository.findById(activityId)).isEmpty();
    }

    // Verifies that orphan removal deletes activities when removed from the stop's activities list
    @Test
    void orphanRemoval_removingActivityFromList_deletesActivity() {
        Stop stop = Stop.builder()
                .day(savedDay).location(savedLocation).stopOrder(1).duration(120).build();

        Activity activity1 = Activity.builder()
                .stop(stop).duration(60).description("Climb the rock").build();
        Activity activity2 = Activity.builder()
                .stop(stop).duration(30).description("Take photos").build();
        stop.getActivities().add(activity1);
        stop.getActivities().add(activity2);

        Stop saved = stopRepository.save(stop);
        assertThat(activityRepository.findAll()).hasSize(2);

        saved.getActivities().remove(0);
        stopRepository.saveAndFlush(saved);

        assertThat(activityRepository.findAll()).hasSize(1);
    }

    // SECURITY: Verifies that deleting a stop from one day does not affect stops in another day
    @Test
    void delete_stop_doesNotAffectOtherDaysStops() {
        Stop stop1 = stopRepository.save(Stop.builder()
                .day(savedDay).location(savedLocation).stopOrder(1).duration(120).build());
        Stop stop2 = stopRepository.save(Stop.builder()
                .day(otherDay).location(savedLocation).stopOrder(1).duration(90).build());

        stopRepository.delete(stop1);
        stopRepository.flush();

        assertThat(stopRepository.findById(stop1.getStopId())).isEmpty();
        assertThat(stopRepository.findById(stop2.getStopId())).isPresent();
    }

    // Verifies that multiple locations can be referenced by different stops
    @Test
    void save_multipleStopsWithDifferentLocations_succeeds() {
        Stop stop1 = stopRepository.save(Stop.builder()
                .day(savedDay).location(savedLocation).stopOrder(1).duration(120).build());
        Stop stop2 = stopRepository.save(Stop.builder()
                .day(savedDay).location(otherLocation).stopOrder(2).duration(60).build());

        assertThat(stop1.getLocation().getLocationId()).isNotEqualTo(stop2.getLocation().getLocationId());
        assertThat(stop1.getLocation().getPlaceName()).isEqualTo("Sigiriya");
        assertThat(stop2.getLocation().getPlaceName()).isEqualTo("Kandy");
    }

    // SECURITY: Verifies that saving a stop with a non-existent day reference throws an exception
    @Test
    void save_withNonExistentDay_throwsException() {
        Day fakeDay = Day.builder()
                .dayId(UUID.randomUUID())
                .dayNo(99)
                .date(LocalDate.of(2026, 12, 1))
                .build();

        Stop stop = Stop.builder()
                .day(fakeDay)
                .location(savedLocation)
                .stopOrder(1)
                .duration(60)
                .build();

        assertThatThrownBy(() -> stopRepository.saveAndFlush(stop))
                .isInstanceOf(Exception.class);
    }

    // SECURITY: Verifies that saving a stop with a non-existent location reference throws an exception
    @Test
    void save_withNonExistentLocation_throwsException() {
        Location fakeLocation = Location.builder()
                .locationId(UUID.randomUUID())
                .placeName("Fake")
                .latitude(BigDecimal.ZERO)
                .longitude(BigDecimal.ZERO)
                .build();

        Stop stop = Stop.builder()
                .day(savedDay)
                .location(fakeLocation)
                .stopOrder(1)
                .duration(60)
                .build();

        assertThatThrownBy(() -> stopRepository.saveAndFlush(stop))
                .isInstanceOf(Exception.class);
    }

    // Verifies that each stop gets a unique UUID, preventing ID collisions
    @Test
    void save_multipleStops_generatesUniqueIds() {
        Stop stop1 = stopRepository.save(Stop.builder()
                .day(savedDay).location(savedLocation).stopOrder(1).duration(120).build());
        Stop stop2 = stopRepository.save(Stop.builder()
                .day(savedDay).location(otherLocation).stopOrder(2).duration(60).build());

        assertThat(stop1.getStopId()).isNotEqualTo(stop2.getStopId());
    }

    // Verifies that updating a stop's duration persists the change correctly
    @Test
    void update_duration_persistsChange() {
        Stop stop = stopRepository.save(Stop.builder()
                .day(savedDay).location(savedLocation).stopOrder(1).duration(120).build());

        stop.setDuration(90);
        stopRepository.saveAndFlush(stop);

        Optional<Stop> updated = stopRepository.findById(stop.getStopId());

        assertThat(updated).isPresent();
        assertThat(updated.get().getDuration()).isEqualTo(90);
    }
}
