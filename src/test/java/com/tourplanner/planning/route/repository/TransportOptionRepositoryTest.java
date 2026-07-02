package com.tourplanner.planning.route.repository;

import com.tourplanner.planning.route.entity.TransportOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransportOptionRepositoryTest {

    @Autowired
    private TransportOptionRepository transportOptionRepository;

    @BeforeEach
    void setUp() {
        transportOptionRepository.deleteAll();
    }

    // Verifies that a transport option is persisted with a generated UUID and correct fields
    @Test
    void save_validTransportOption_persistsWithGeneratedId() {
        TransportOption transport = transportOptionRepository.save(TransportOption.builder()
                .type("bus")
                .label("Public Bus")
                .build());

        assertThat(transport.getTransportId()).isNotNull();
        assertThat(transport.getType()).isEqualTo("bus");
        assertThat(transport.getLabel()).isEqualTo("Public Bus");
    }

    // Verifies that findByType returns the correct transport option
    @Test
    void findByType_existingType_returnsTransportOption() {
        transportOptionRepository.save(TransportOption.builder()
                .type("train")
                .label("Express Train")
                .build());

        Optional<TransportOption> found = transportOptionRepository.findByType("train");

        assertThat(found).isPresent();
        assertThat(found.get().getType()).isEqualTo("train");
        assertThat(found.get().getLabel()).isEqualTo("Express Train");
    }

    // Verifies that findByType returns empty for a non-existent type
    @Test
    void findByType_nonExistingType_returnsEmpty() {
        Optional<TransportOption> found = transportOptionRepository.findByType("helicopter");

        assertThat(found).isEmpty();
    }

    // Verifies that findById returns the correct transport option
    @Test
    void findById_existingId_returnsTransportOption() {
        TransportOption saved = transportOptionRepository.save(TransportOption.builder()
                .type("taxi")
                .label("Private Taxi")
                .build());

        Optional<TransportOption> found = transportOptionRepository.findById(saved.getTransportId());

        assertThat(found).isPresent();
        assertThat(found.get().getType()).isEqualTo("taxi");
    }

    // Verifies that findById returns empty for a non-existent UUID
    @Test
    void findById_nonExistingId_returnsEmpty() {
        Optional<TransportOption> found = transportOptionRepository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    // Verifies that each transport option gets a unique UUID
    @Test
    void save_multipleTransportOptions_generatesUniqueIds() {
        TransportOption t1 = transportOptionRepository.save(TransportOption.builder()
                .type("bus").label("Public Bus").build());
        TransportOption t2 = transportOptionRepository.save(TransportOption.builder()
                .type("train").label("Express Train").build());

        assertThat(t1.getTransportId()).isNotEqualTo(t2.getTransportId());
    }
}
