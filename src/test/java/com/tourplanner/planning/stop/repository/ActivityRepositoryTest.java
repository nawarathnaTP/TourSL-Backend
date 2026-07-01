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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ActivityRepositoryTest {

    @Autowired
    private ActivityRepository activityRepository;

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

    private Stop savedStop;
    private Stop otherStop;

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

        Day day = dayRepository.save(Day.builder()
                .tour(tour)
                .dayNo(1)
                .date(LocalDate.of(2026, 7, 1))
                .build());

        Location location = locationRepository.save(Location.builder()
                .placeName("Sigiriya")
                .latitude(BigDecimal.valueOf(7.957))
                .longitude(BigDecimal.valueOf(80.760))
                .build());

        savedStop = stopRepository.save(Stop.builder()
                .day(day)
                .location(location)
                .stopOrder(1)
                .duration(120)
                .build());

        otherStop = stopRepository.save(Stop.builder()
                .day(day)
                .location(location)
                .stopOrder(2)
                .duration(60)
                .build());
    }

    // Verifies that an activity is persisted with a generated UUID and correct fields
    @Test
    void save_validActivity_persistsWithGeneratedId() {
        Activity activity = activityRepository.save(Activity.builder()
                .stop(savedStop)
                .duration(60)
                .description("Climb the rock")
                .build());

        assertThat(activity.getActivityId()).isNotNull();
        assertThat(activity.getStop().getStopId()).isEqualTo(savedStop.getStopId());
        assertThat(activity.getDuration()).isEqualTo(60);
        assertThat(activity.getDescription()).isEqualTo("Climb the rock");
    }

    // Verifies that saving an activity without a stop reference violates the NOT NULL constraint
    @Test
    void save_withoutStop_throwsDataIntegrityViolation() {
        Activity activity = Activity.builder()
                .duration(60)
                .description("Test activity")
                .build();

        assertThatThrownBy(() -> activityRepository.saveAndFlush(activity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that saving an activity without a duration violates the NOT NULL constraint
    @Test
    void save_withoutDuration_throwsDataIntegrityViolation() {
        Activity activity = Activity.builder()
                .stop(savedStop)
                .description("Test activity")
                .build();

        assertThatThrownBy(() -> activityRepository.saveAndFlush(activity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that saving an activity without a description violates the NOT NULL constraint
    @Test
    void save_withoutDescription_throwsDataIntegrityViolation() {
        Activity activity = Activity.builder()
                .stop(savedStop)
                .duration(60)
                .build();

        assertThatThrownBy(() -> activityRepository.saveAndFlush(activity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that findById returns the correct activity when it exists
    @Test
    void findById_existingActivity_returnsActivity() {
        Activity saved = activityRepository.save(Activity.builder()
                .stop(savedStop)
                .duration(60)
                .description("Climb the rock")
                .build());

        Optional<Activity> found = activityRepository.findById(saved.getActivityId());

        assertThat(found).isPresent();
        assertThat(found.get().getActivityId()).isEqualTo(saved.getActivityId());
        assertThat(found.get().getDescription()).isEqualTo("Climb the rock");
    }

    // Verifies that findById returns empty for a non-existent UUID
    @Test
    void findById_nonExistingId_returnsEmpty() {
        Optional<Activity> found = activityRepository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    // Verifies that bookingId can be set and persisted on an activity
    @Test
    void save_withBookingId_persistsBookingId() {
        UUID bookingId = UUID.randomUUID();
        Activity activity = activityRepository.save(Activity.builder()
                .stop(savedStop)
                .duration(60)
                .description("Guided tour")
                .bookingId(bookingId)
                .build());

        Optional<Activity> found = activityRepository.findById(activity.getActivityId());

        assertThat(found).isPresent();
        assertThat(found.get().getBookingId()).isEqualTo(bookingId);
    }

    // Verifies that bookingId is nullable and can be persisted as null
    @Test
    void save_withNullBookingId_persistsSuccessfully() {
        Activity activity = activityRepository.save(Activity.builder()
                .stop(savedStop)
                .duration(60)
                .description("Free activity")
                .bookingId(null)
                .build());

        Optional<Activity> found = activityRepository.findById(activity.getActivityId());

        assertThat(found).isPresent();
        assertThat(found.get().getBookingId()).isNull();
    }

    // Verifies that multiple activities can be added to the same stop
    @Test
    void save_multipleActivitiesSameStop_succeeds() {
        Activity activity1 = activityRepository.save(Activity.builder()
                .stop(savedStop).duration(60).description("Climb the rock").build());
        Activity activity2 = activityRepository.save(Activity.builder()
                .stop(savedStop).duration(30).description("Take photos").build());
        Activity activity3 = activityRepository.save(Activity.builder()
                .stop(savedStop).duration(15).description("Buy souvenirs").build());

        assertThat(activityRepository.findAll().stream()
                .filter(a -> a.getStop().getStopId().equals(savedStop.getStopId()))
                .toList()).hasSize(3);
    }

    // SECURITY: Verifies that deleting an activity from one stop does not affect activities in another stop
    @Test
    void delete_activity_doesNotAffectOtherStopsActivities() {
        Activity activity1 = activityRepository.save(Activity.builder()
                .stop(savedStop).duration(60).description("Climb the rock").build());
        Activity activity2 = activityRepository.save(Activity.builder()
                .stop(otherStop).duration(30).description("Take photos").build());

        activityRepository.delete(activity1);
        activityRepository.flush();

        assertThat(activityRepository.findById(activity1.getActivityId())).isEmpty();
        assertThat(activityRepository.findById(activity2.getActivityId())).isPresent();
    }

    // SECURITY: Verifies that saving an activity with a non-existent stop reference throws an exception
    @Test
    void save_withNonExistentStop_throwsException() {
        Stop fakeStop = Stop.builder()
                .stopId(UUID.randomUUID())
                .stopOrder(99)
                .duration(60)
                .build();

        Activity activity = Activity.builder()
                .stop(fakeStop)
                .duration(60)
                .description("Test")
                .build();

        assertThatThrownBy(() -> activityRepository.saveAndFlush(activity))
                .isInstanceOf(Exception.class);
    }

    // Verifies that each activity gets a unique UUID, preventing ID collisions
    @Test
    void save_multipleActivities_generatesUniqueIds() {
        Activity activity1 = activityRepository.save(Activity.builder()
                .stop(savedStop).duration(60).description("Activity 1").build());
        Activity activity2 = activityRepository.save(Activity.builder()
                .stop(savedStop).duration(30).description("Activity 2").build());

        assertThat(activity1.getActivityId()).isNotEqualTo(activity2.getActivityId());
    }

    // Verifies that updating an activity's description persists the change correctly
    @Test
    void update_description_persistsChange() {
        Activity activity = activityRepository.save(Activity.builder()
                .stop(savedStop).duration(60).description("Original").build());

        activity.setDescription("Updated description");
        activityRepository.saveAndFlush(activity);

        Optional<Activity> updated = activityRepository.findById(activity.getActivityId());

        assertThat(updated).isPresent();
        assertThat(updated.get().getDescription()).isEqualTo("Updated description");
    }

    // Verifies that updating an activity's duration persists the change correctly
    @Test
    void update_duration_persistsChange() {
        Activity activity = activityRepository.save(Activity.builder()
                .stop(savedStop).duration(60).description("Test").build());

        activity.setDuration(90);
        activityRepository.saveAndFlush(activity);

        Optional<Activity> updated = activityRepository.findById(activity.getActivityId());

        assertThat(updated).isPresent();
        assertThat(updated.get().getDuration()).isEqualTo(90);
    }

    // Verifies that updating bookingId from null to a value persists correctly
    @Test
    void update_bookingId_persistsChange() {
        Activity activity = activityRepository.save(Activity.builder()
                .stop(savedStop).duration(60).description("Test").build());

        UUID bookingId = UUID.randomUUID();
        activity.setBookingId(bookingId);
        activityRepository.saveAndFlush(activity);

        Optional<Activity> updated = activityRepository.findById(activity.getActivityId());

        assertThat(updated).isPresent();
        assertThat(updated.get().getBookingId()).isEqualTo(bookingId);
    }
}
