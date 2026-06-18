package com.tourplanner.planning.repository;

import com.tourplanner.planning.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	private User savedUser;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
		savedUser = userRepository.save(User.builder()
				.firstName("John")
				.lastName("Doe")
				.email("john@example.com")
				.build());
	}

	@Test
	void findByEmail_existingEmail_returnsUser() {
		Optional<User> found = userRepository.findByEmail("john@example.com");

		assertThat(found).isPresent();
		assertThat(found.get().getFirstName()).isEqualTo("John");
		assertThat(found.get().getLastName()).isEqualTo("Doe");
	}

	@Test
	void findByEmail_nonExistingEmail_returnsEmpty() {
		Optional<User> found = userRepository.findByEmail("nobody@example.com");

		assertThat(found).isEmpty();
	}

	@Test
	void existsByEmail_existingEmail_returnsTrue() {
		assertThat(userRepository.existsByEmail("john@example.com")).isTrue();
	}

	@Test
	void existsByEmail_nonExistingEmail_returnsFalse() {
		assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
	}

	@Test
	void save_setsCreatedAtAndUpdatedAt() {
		assertThat(savedUser.getId()).isNotNull();
		assertThat(savedUser.getCreatedAt()).isNotNull();
		assertThat(savedUser.getUpdatedAt()).isNotNull();
	}
}
