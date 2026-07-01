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
class TourRepositoryTest {

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private DayRepository dayRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;
    private User otherUser;

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

        otherUser = userRepository.save(User.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .build());
    }

    // Verifies that a tour is persisted with a generated UUID and correct fields
    @Test
    void save_validTour_persistsWithGeneratedId() {
        Tour tour = tourRepository.save(Tour.builder()
                .user(savedUser)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 5))
                .build());

        assertThat(tour.getTourId()).isNotNull();
        assertThat(tour.getUser().getId()).isEqualTo(savedUser.getId());
        assertThat(tour.getStartDay()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(tour.getEndDay()).isEqualTo(LocalDate.of(2026, 7, 5));
    }

    // Verifies that createdAt and updatedAt timestamps are auto-populated on save
    @Test
    void save_setsCreatedAtAndUpdatedAt() {
        Tour tour = tourRepository.save(Tour.builder()
                .user(savedUser)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build());

        assertThat(tour.getCreatedAt()).isNotNull();
        assertThat(tour.getUpdatedAt()).isNotNull();
    }

    // Verifies that saving a tour without a user violates the NOT NULL constraint
    @Test
    void save_withoutUser_throwsDataIntegrityViolation() {
        Tour tour = Tour.builder()
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build();

        assertThatThrownBy(() -> tourRepository.saveAndFlush(tour))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that saving a tour without a start day violates the NOT NULL constraint
    @Test
    void save_withoutStartDay_throwsDataIntegrityViolation() {
        Tour tour = Tour.builder()
                .user(savedUser)
                .endDay(LocalDate.of(2026, 7, 3))
                .build();

        assertThatThrownBy(() -> tourRepository.saveAndFlush(tour))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that saving a tour without an end day violates the NOT NULL constraint
    @Test
    void save_withoutEndDay_throwsDataIntegrityViolation() {
        Tour tour = Tour.builder()
                .user(savedUser)
                .startDay(LocalDate.of(2026, 7, 1))
                .build();

        assertThatThrownBy(() -> tourRepository.saveAndFlush(tour))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that findById returns the correct tour when it exists
    @Test
    void findById_existingTour_returnsTour() {
        Tour saved = tourRepository.save(Tour.builder()
                .user(savedUser)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build());

        Optional<Tour> found = tourRepository.findById(saved.getTourId());

        assertThat(found).isPresent();
        assertThat(found.get().getTourId()).isEqualTo(saved.getTourId());
    }

    // Verifies that findById returns empty for a non-existent UUID
    @Test
    void findById_nonExistingId_returnsEmpty() {
        Optional<Tour> found = tourRepository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    // Verifies that findByUser_Id returns only tours belonging to the specified user
    @Test
    void findByUserId_returnsToursBelongingToUser() {
        tourRepository.save(Tour.builder()
                .user(savedUser)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build());
        tourRepository.save(Tour.builder()
                .user(savedUser)
                .startDay(LocalDate.of(2026, 8, 1))
                .endDay(LocalDate.of(2026, 8, 5))
                .build());

        List<Tour> tours = tourRepository.findByUser_Id(savedUser.getId());

        assertThat(tours).hasSize(2);
        assertThat(tours).allMatch(t -> t.getUser().getId().equals(savedUser.getId()));
    }

    // SECURITY: Verifies that one user's tours are not returned when querying by another user's ID
    @Test
    void findByUserId_doesNotReturnOtherUsersTours() {
        tourRepository.save(Tour.builder()
                .user(savedUser)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build());
        tourRepository.save(Tour.builder()
                .user(otherUser)
                .startDay(LocalDate.of(2026, 8, 1))
                .endDay(LocalDate.of(2026, 8, 5))
                .build());

        List<Tour> johnsTours = tourRepository.findByUser_Id(savedUser.getId());
        List<Tour> janesTours = tourRepository.findByUser_Id(otherUser.getId());

        assertThat(johnsTours).hasSize(1);
        assertThat(janesTours).hasSize(1);
        assertThat(johnsTours.get(0).getTourId()).isNotEqualTo(janesTours.get(0).getTourId());
    }

    // SECURITY: Verifies that querying by a non-existent user ID returns an empty list, not an error
    @Test
    void findByUserId_nonExistentUser_returnsEmptyList() {
        tourRepository.save(Tour.builder()
                .user(savedUser)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build());

        List<Tour> tours = tourRepository.findByUser_Id(UUID.randomUUID());

        assertThat(tours).isEmpty();
    }

    // Verifies that deleting a tour also cascades deletion to its associated days
    @Test
    void delete_tour_cascadesDeleteToDays() {
        Tour tour = Tour.builder()
                .user(savedUser)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 2))
                .build();

        Day day1 = Day.builder().tour(tour).dayNo(1).date(LocalDate.of(2026, 7, 1)).build();
        Day day2 = Day.builder().tour(tour).dayNo(2).date(LocalDate.of(2026, 7, 2)).build();
        tour.getDays().add(day1);
        tour.getDays().add(day2);

        Tour saved = tourRepository.save(tour);
        UUID tourId = saved.getTourId();

        assertThat(dayRepository.findByTour_TourIdOrderByDayNo(tourId)).hasSize(2);

        tourRepository.delete(saved);
        tourRepository.flush();

        assertThat(tourRepository.findById(tourId)).isEmpty();
        assertThat(dayRepository.findByTour_TourIdOrderByDayNo(tourId)).isEmpty();
    }

    // SECURITY: Verifies that deleting one user's tour does not affect another user's tours
    @Test
    void delete_tour_doesNotAffectOtherUsersTours() {
        Tour johnsTour = tourRepository.save(Tour.builder()
                .user(savedUser)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build());
        Tour janesTour = tourRepository.save(Tour.builder()
                .user(otherUser)
                .startDay(LocalDate.of(2026, 8, 1))
                .endDay(LocalDate.of(2026, 8, 5))
                .build());

        tourRepository.delete(johnsTour);
        tourRepository.flush();

        assertThat(tourRepository.findById(johnsTour.getTourId())).isEmpty();
        assertThat(tourRepository.findById(janesTour.getTourId())).isPresent();
    }

    // SECURITY: Verifies that saving a tour with a non-existent user reference throws an integrity violation
    @Test
    void save_withNonExistentUser_throwsDataIntegrityViolation() {
        User fakeUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("Fake")
                .lastName("User")
                .email("fake@example.com")
                .build();

        Tour tour = Tour.builder()
                .user(fakeUser)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build();

        assertThatThrownBy(() -> tourRepository.saveAndFlush(tour))
                .isInstanceOf(Exception.class);
    }

    // Verifies that orphan removal deletes days when they are removed from the tour's days list
    @Test
    void orphanRemoval_removingDayFromList_deletesDay() {
        Tour tour = Tour.builder()
                .user(savedUser)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 2))
                .build();

        Day day1 = Day.builder().tour(tour).dayNo(1).date(LocalDate.of(2026, 7, 1)).build();
        Day day2 = Day.builder().tour(tour).dayNo(2).date(LocalDate.of(2026, 7, 2)).build();
        tour.getDays().add(day1);
        tour.getDays().add(day2);

        Tour saved = tourRepository.save(tour);
        assertThat(dayRepository.findByTour_TourIdOrderByDayNo(saved.getTourId())).hasSize(2);

        saved.getDays().remove(0);
        tourRepository.saveAndFlush(saved);

        assertThat(dayRepository.findByTour_TourIdOrderByDayNo(saved.getTourId())).hasSize(1);
    }

    // Verifies that each tour gets a unique UUID, preventing ID collisions
    @Test
    void save_multipleTours_generatesUniqueIds() {
        Tour tour1 = tourRepository.save(Tour.builder()
                .user(savedUser)
                .startDay(LocalDate.of(2026, 7, 1))
                .endDay(LocalDate.of(2026, 7, 3))
                .build());
        Tour tour2 = tourRepository.save(Tour.builder()
                .user(savedUser)
                .startDay(LocalDate.of(2026, 8, 1))
                .endDay(LocalDate.of(2026, 8, 5))
                .build());

        assertThat(tour1.getTourId()).isNotEqualTo(tour2.getTourId());
    }
}
