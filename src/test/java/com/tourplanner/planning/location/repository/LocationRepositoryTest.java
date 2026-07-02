package com.tourplanner.planning.location.repository;

import com.tourplanner.planning.location.entity.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LocationRepositoryTest {

    @Autowired
    private LocationRepository locationRepository;

    @BeforeEach
    void setUp() {
        locationRepository.deleteAll();
    }

    // Verifies that a location is persisted with a generated UUID and correct fields
    @Test
    void save_validLocation_persistsWithGeneratedId() {
        Location location = locationRepository.save(Location.builder()
                .externalId("ext-123")
                .placeName("Sigiriya")
                .latitude(BigDecimal.valueOf(7.957))
                .longitude(BigDecimal.valueOf(80.760))
                .imageUrl("https://example.com/sigiriya.jpg")
                .build());

        assertThat(location.getLocationId()).isNotNull();
        assertThat(location.getExternalId()).isEqualTo("ext-123");
        assertThat(location.getPlaceName()).isEqualTo("Sigiriya");
        assertThat(location.getLatitude()).isEqualByComparingTo(BigDecimal.valueOf(7.957));
        assertThat(location.getLongitude()).isEqualByComparingTo(BigDecimal.valueOf(80.760));
        assertThat(location.getImageUrl()).isEqualTo("https://example.com/sigiriya.jpg");
    }

    // Verifies that saving a location without a placeName throws a DataIntegrityViolation
    @Test
    void save_withoutPlaceName_throwsDataIntegrityViolation() {
        Location location = Location.builder()
                .latitude(BigDecimal.valueOf(7.957))
                .longitude(BigDecimal.valueOf(80.760))
                .build();

        assertThatThrownBy(() -> locationRepository.saveAndFlush(location))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that saving a location without latitude throws a DataIntegrityViolation
    @Test
    void save_withoutLatitude_throwsDataIntegrityViolation() {
        Location location = Location.builder()
                .placeName("Sigiriya")
                .longitude(BigDecimal.valueOf(80.760))
                .build();

        assertThatThrownBy(() -> locationRepository.saveAndFlush(location))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that saving a location without longitude throws a DataIntegrityViolation
    @Test
    void save_withoutLongitude_throwsDataIntegrityViolation() {
        Location location = Location.builder()
                .placeName("Sigiriya")
                .latitude(BigDecimal.valueOf(7.957))
                .build();

        assertThatThrownBy(() -> locationRepository.saveAndFlush(location))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies that a location can be saved without an externalId (it's optional)
    @Test
    void save_withoutExternalId_succeeds() {
        Location location = locationRepository.save(Location.builder()
                .placeName("Custom Spot")
                .latitude(BigDecimal.valueOf(6.927))
                .longitude(BigDecimal.valueOf(79.861))
                .build());

        assertThat(location.getLocationId()).isNotNull();
        assertThat(location.getExternalId()).isNull();
    }

    // Verifies that a location can be saved without an imageUrl (it's optional)
    @Test
    void save_withoutImageUrl_succeeds() {
        Location location = locationRepository.save(Location.builder()
                .placeName("Kandy")
                .latitude(BigDecimal.valueOf(7.291))
                .longitude(BigDecimal.valueOf(80.636))
                .build());

        assertThat(location.getLocationId()).isNotNull();
        assertThat(location.getImageUrl()).isNull();
    }

    // Verifies that findById returns the correct location when it exists
    @Test
    void findById_existingLocation_returnsLocation() {
        Location saved = locationRepository.save(Location.builder()
                .placeName("Sigiriya")
                .latitude(BigDecimal.valueOf(7.957))
                .longitude(BigDecimal.valueOf(80.760))
                .build());

        Optional<Location> found = locationRepository.findById(saved.getLocationId());

        assertThat(found).isPresent();
        assertThat(found.get().getPlaceName()).isEqualTo("Sigiriya");
    }

    // Verifies that findById returns empty for a non-existent UUID
    @Test
    void findById_nonExistingId_returnsEmpty() {
        Optional<Location> found = locationRepository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    // Verifies that findByExternalId returns the correct location
    @Test
    void findByExternalId_existingExternalId_returnsLocation() {
        locationRepository.save(Location.builder()
                .externalId("ext-123")
                .placeName("Sigiriya")
                .latitude(BigDecimal.valueOf(7.957))
                .longitude(BigDecimal.valueOf(80.760))
                .build());

        Optional<Location> found = locationRepository.findByExternalId("ext-123");

        assertThat(found).isPresent();
        assertThat(found.get().getPlaceName()).isEqualTo("Sigiriya");
        assertThat(found.get().getExternalId()).isEqualTo("ext-123");
    }

    // Verifies that findByExternalId returns empty for a non-existent externalId
    @Test
    void findByExternalId_nonExistingExternalId_returnsEmpty() {
        Optional<Location> found = locationRepository.findByExternalId("non-existent");

        assertThat(found).isEmpty();
    }

    // Verifies that each location gets a unique UUID
    @Test
    void save_multipleLocations_generatesUniqueIds() {
        Location l1 = locationRepository.save(Location.builder()
                .placeName("Sigiriya").latitude(BigDecimal.valueOf(7.957)).longitude(BigDecimal.valueOf(80.760)).build());
        Location l2 = locationRepository.save(Location.builder()
                .placeName("Kandy").latitude(BigDecimal.valueOf(7.291)).longitude(BigDecimal.valueOf(80.636)).build());

        assertThat(l1.getLocationId()).isNotEqualTo(l2.getLocationId());
    }

    // Verifies that updating a location's placeName persists the change
    @Test
    void update_placeName_persistsChange() {
        Location location = locationRepository.save(Location.builder()
                .placeName("Sigiriya")
                .latitude(BigDecimal.valueOf(7.957))
                .longitude(BigDecimal.valueOf(80.760))
                .build());

        location.setPlaceName("Sigiriya Rock Fortress");
        locationRepository.saveAndFlush(location);

        Optional<Location> updated = locationRepository.findById(location.getLocationId());

        assertThat(updated).isPresent();
        assertThat(updated.get().getPlaceName()).isEqualTo("Sigiriya Rock Fortress");
    }

    // Verifies that deleting a location does not affect other locations
    @Test
    void delete_location_doesNotAffectOtherLocations() {
        Location l1 = locationRepository.save(Location.builder()
                .placeName("Sigiriya").latitude(BigDecimal.valueOf(7.957)).longitude(BigDecimal.valueOf(80.760)).build());
        Location l2 = locationRepository.save(Location.builder()
                .placeName("Kandy").latitude(BigDecimal.valueOf(7.291)).longitude(BigDecimal.valueOf(80.636)).build());

        locationRepository.delete(l1);
        locationRepository.flush();

        assertThat(locationRepository.findById(l1.getLocationId())).isEmpty();
        assertThat(locationRepository.findById(l2.getLocationId())).isPresent();
    }
}
