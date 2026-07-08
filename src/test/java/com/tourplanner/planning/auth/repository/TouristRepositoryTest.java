package com.tourplanner.planning.auth.repository;

import com.tourplanner.planning.auth.entity.Role;
import com.tourplanner.planning.auth.entity.Tourist;
import com.tourplanner.planning.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TouristRepositoryTest {

	@Autowired
	private TouristRepository touristRepository;

	@Autowired
	private UserRepository userRepository;

	private User savedUser;

	@BeforeEach
	void setUp() {
		touristRepository.deleteAll();
		userRepository.deleteAll();

		savedUser = userRepository.save(User.builder()
				.firstName("John")
				.lastName("Doe")
				.email("john@example.com")
				.role(Role.TOURIST)
				.build());
	}

	@Test
	void save_createsTouristProfile() {
		Tourist tourist = touristRepository.save(Tourist.builder()
				.user(savedUser)
				.language("English")
				.nationality("British")
				.build());

		assertThat(tourist.getTouristId()).isNotNull();
		assertThat(tourist.getLanguage()).isEqualTo("English");
		assertThat(tourist.getNationality()).isEqualTo("British");
	}

	@Test
	void findByUser_Id_existingUser_returnsTourist() {
		touristRepository.save(Tourist.builder()
				.user(savedUser)
				.language("English")
				.nationality("British")
				.build());

		Optional<Tourist> found = touristRepository.findByUser_Id(savedUser.getId());

		assertThat(found).isPresent();
		assertThat(found.get().getLanguage()).isEqualTo("English");
		assertThat(found.get().getNationality()).isEqualTo("British");
	}

	@Test
	void findByUser_Id_nonExistingUser_returnsEmpty() {
		Optional<Tourist> found = touristRepository.findByUser_Id(java.util.UUID.randomUUID());

		assertThat(found).isEmpty();
	}

	@Test
	void save_nullFields_createsProfileWithNulls() {
		Tourist tourist = touristRepository.save(Tourist.builder()
				.user(savedUser)
				.build());

		assertThat(tourist.getTouristId()).isNotNull();
		assertThat(tourist.getLanguage()).isNull();
		assertThat(tourist.getNationality()).isNull();
	}
}
