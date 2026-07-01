package com.tourplanner.planning.tour.repository;

import com.tourplanner.planning.auth.entity.User;
import com.tourplanner.planning.auth.repository.UserRepository;
import com.tourplanner.planning.tour.entity.Day;
import com.tourplanner.planning.tour.entity.Tour;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DayRepositoryTest {

    @Autowired
    private DayRepository dayRepository;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;
    private Tour savedTour;
    private Tour otherTour;

    @BeforeEach
    void setUp() {
        dayRepository.deleteAll();
        tourRepository.deleteAll();
        userRepository.deleteAll();

        savedUser = userRepository.save(User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build());

        User otherUser = userRepository.save(User.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .build());

        savedTour = tourRepository.save(Tour.builder()
                .user(savedUser)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build());

        otherTour = tourRepository.save(Tour.builder()
                .user(otherUser)
                .startDay(LocalDate.of(2026, 8, 1))
                .endDay(LocalDate.of(2026, 8, 3))
                .build());
    }

    // Verifies that a day is persisted with a generated UUID and correct fields
    @Test
    void save_validDay_persistsWithGeneratedId() {
        Day day = dayRepository.save(Day.builder()
                .tour(savedTour)
                .dayNo(1)
                .date(LocalDate.of(2026, 7, 1))
                .build());

        assertThat(day.getDayId()).isNotNull();
        assertThat(day.getTour().getTourId()).isEqualTo(savedTour.getTourId());
        assertThat(day.getDayNo()).isEqualTo(1);
        assertThat(day.getDate()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    // Verifies that saving a day without a tour reference violates the NOT NULL constraint
    @Test
    void save_withoutTour_throwsDataIntegrityViolation() {
        Day day = Day.builder()
                .dayNo(1)
                .date(LocalDate.of(2026, 7, 1))
                .build();

        assertThatThrownBy(() -> dayRepository.saveAndFlush(day))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that saving a day without a day number violates the NOT NULL constraint
    @Test
    void save_withoutDayNo_throwsDataIntegrityViolation() {
        Day day = Day.builder()
                .tour(savedTour)
                .date(LocalDate.of(2026, 7, 1))
                .build();

        assertThatThrownBy(() -> dayRepository.saveAndFlush(day))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that saving a day without a date violates the NOT NULL constraint
    @Test
    void save_withoutDate_throwsDataIntegrityViolation() {
        Day day = Day.builder()
                .tour(savedTour)
                .dayNo(1)
                .build();

        assertThatThrownBy(() -> dayRepository.saveAndFlush(day))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that findById returns the correct day when it exists
    @Test
    void findById_existingDay_returnsDay() {
        Day saved = dayRepository.save(Day.builder()
                .tour(savedTour)
                .dayNo(1)
                .date(LocalDate.of(2026, 7, 1))
                .build());

        Optional<Day> found = dayRepository.findById(saved.getDayId());

        assertThat(found).isPresent();
        assertThat(found.get().getDayId()).isEqualTo(saved.getDayId());
    }

    // Verifies that findById returns empty for a non-existent UUID
    @Test
    void findById_nonExistingId_returnsEmpty() {
        Optional<Day> found = dayRepository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    // Verifies that findByTour_TourIdOrderByDayNo returns days sorted by day number
    @Test
    void findByTourId_returnsDaysOrderedByDayNo() {
        dayRepository.save(Day.builder().tour(savedTour).dayNo(3).date(LocalDate.of(2026, 7, 3)).build());
        dayRepository.save(Day.builder().tour(savedTour).dayNo(1).date(LocalDate.of(2026, 7, 1)).build());
        dayRepository.save(Day.builder().tour(savedTour).dayNo(2).date(LocalDate.of(2026, 7, 2)).build());

        List<Day> days = dayRepository.findByTour_TourIdOrderByDayNo(savedTour.getTourId());

        assertThat(days).hasSize(3);
        assertThat(days.get(0).getDayNo()).isEqualTo(1);
        assertThat(days.get(1).getDayNo()).isEqualTo(2);
        assertThat(days.get(2).getDayNo()).isEqualTo(3);
    }

    // SECURITY: Verifies that querying days by tour ID only returns days for that specific tour
    @Test
    void findByTourId_doesNotReturnDaysFromOtherTours() {
        dayRepository.save(Day.builder().tour(savedTour).dayNo(1).date(LocalDate.of(2026, 7, 1)).build());
        dayRepository.save(Day.builder().tour(savedTour).dayNo(2).date(LocalDate.of(2026, 7, 2)).build());
        dayRepository.save(Day.builder().tour(otherTour).dayNo(1).date(LocalDate.of(2026, 8, 1)).build());

        List<Day> savedTourDays = dayRepository.findByTour_TourIdOrderByDayNo(savedTour.getTourId());
        List<Day> otherTourDays = dayRepository.findByTour_TourIdOrderByDayNo(otherTour.getTourId());

        assertThat(savedTourDays).hasSize(2);
        assertThat(otherTourDays).hasSize(1);
        assertThat(savedTourDays).allMatch(d -> d.getTour().getTourId().equals(savedTour.getTourId()));
        assertThat(otherTourDays).allMatch(d -> d.getTour().getTourId().equals(otherTour.getTourId()));
    }

    // SECURITY: Verifies that querying days by a non-existent tour ID returns an empty list
    @Test
    void findByTourId_nonExistentTour_returnsEmptyList() {
        dayRepository.save(Day.builder().tour(savedTour).dayNo(1).date(LocalDate.of(2026, 7, 1)).build());

        List<Day> days = dayRepository.findByTour_TourIdOrderByDayNo(UUID.randomUUID());

        assertThat(days).isEmpty();
    }

    // Verifies that lodgingId can be set and persisted on a day
    @Test
    void save_withLodgingId_persistsLodging() {
        UUID lodgingId = UUID.randomUUID();
        Day day = dayRepository.save(Day.builder()
                .tour(savedTour)
                .dayNo(1)
                .date(LocalDate.of(2026, 7, 1))
                .lodgingId(lodgingId)
                .build());

        Optional<Day> found = dayRepository.findById(day.getDayId());

        assertThat(found).isPresent();
        assertThat(found.get().getLodgingId()).isEqualTo(lodgingId);
    }

    // Verifies that lodgingId is nullable and can be persisted as null
    @Test
    void save_withNullLodgingId_persistsSuccessfully() {
        Day day = dayRepository.save(Day.builder()
                .tour(savedTour)
                .dayNo(1)
                .date(LocalDate.of(2026, 7, 1))
                .lodgingId(null)
                .build());

        Optional<Day> found = dayRepository.findById(day.getDayId());

        assertThat(found).isPresent();
        assertThat(found.get().getLodgingId()).isNull();
    }

    // Verifies that each day gets a unique UUID, preventing ID collisions
    @Test
    void save_multipleDays_generatesUniqueIds() {
        Day day1 = dayRepository.save(Day.builder()
                .tour(savedTour).dayNo(1).date(LocalDate.of(2026, 7, 1)).build());
        Day day2 = dayRepository.save(Day.builder()
                .tour(savedTour).dayNo(2).date(LocalDate.of(2026, 7, 2)).build());

        assertThat(day1.getDayId()).isNotEqualTo(day2.getDayId());
    }

    // SECURITY: Verifies that saving a day with a non-existent tour reference throws an integrity violation
    @Test
    void save_withNonExistentTour_throwsException() {
        Tour fakeTour = Tour.builder()
                .tourId(UUID.randomUUID())
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build();

        Day day = Day.builder()
                .tour(fakeTour)
                .dayNo(1)
                .date(LocalDate.of(2026, 7, 1))
                .build();

        assertThatThrownBy(() -> dayRepository.saveAndFlush(day))
                .isInstanceOf(Exception.class);
    }

    // SECURITY: Verifies that deleting a day from one tour does not affect days in another tour
    @Test
    void delete_day_doesNotAffectOtherToursDays() {
        Day day1 = dayRepository.save(Day.builder()
                .tour(savedTour).dayNo(1).date(LocalDate.of(2026, 7, 1)).build());
        Day otherDay = dayRepository.save(Day.builder()
                .tour(otherTour).dayNo(1).date(LocalDate.of(2026, 8, 1)).build());

        dayRepository.delete(day1);
        dayRepository.flush();

        assertThat(dayRepository.findById(day1.getDayId())).isEmpty();
        assertThat(dayRepository.findById(otherDay.getDayId())).isPresent();
    }

    // Verifies that updating a day's lodgingId persists the change correctly
    @Test
    void update_lodgingId_persistsChange() {
        Day day = dayRepository.save(Day.builder()
                .tour(savedTour).dayNo(1).date(LocalDate.of(2026, 7, 1)).build());

        UUID newLodgingId = UUID.randomUUID();
        day.setLodgingId(newLodgingId);
        dayRepository.saveAndFlush(day);

        Optional<Day> updated = dayRepository.findById(day.getDayId());

        assertThat(updated).isPresent();
        assertThat(updated.get().getLodgingId()).isEqualTo(newLodgingId);
    }
}
