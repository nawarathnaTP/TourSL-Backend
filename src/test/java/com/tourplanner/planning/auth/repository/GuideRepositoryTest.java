package com.tourplanner.planning.auth.repository;

import com.tourplanner.planning.auth.entity.Guide;
import com.tourplanner.planning.auth.entity.Role;
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
class GuideRepositoryTest {

	@Autowired
	private GuideRepository guideRepository;

	@Autowired
	private UserRepository userRepository;

	private User savedUser;

	@BeforeEach
	void setUp() {
		guideRepository.deleteAll();
		userRepository.deleteAll();

		savedUser = userRepository.save(User.builder()
				.firstName("Kamal")
				.lastName("Perera")
				.email("kamal@example.com")
				.role(Role.GUIDE)
				.build());
	}

	@Test
	void save_createsGuideProfile() {
		Guide guide = guideRepository.save(Guide.builder()
				.user(savedUser)
				.bio("Experienced guide")
				.specializations("Cultural, Wildlife")
				.licenseNo("SLTDA-12345")
				.rating(4.5)
				.build());

		assertThat(guide.getGuideId()).isNotNull();
		assertThat(guide.getBio()).isEqualTo("Experienced guide");
		assertThat(guide.getSpecializations()).isEqualTo("Cultural, Wildlife");
		assertThat(guide.getLicenseNo()).isEqualTo("SLTDA-12345");
		assertThat(guide.getRating()).isEqualTo(4.5);
	}

	@Test
	void findByUser_Id_existingUser_returnsGuide() {
		guideRepository.save(Guide.builder()
				.user(savedUser)
				.bio("Experienced guide")
				.specializations("Cultural")
				.licenseNo("SLTDA-12345")
				.build());

		Optional<Guide> found = guideRepository.findByUser_Id(savedUser.getId());

		assertThat(found).isPresent();
		assertThat(found.get().getBio()).isEqualTo("Experienced guide");
		assertThat(found.get().getLicenseNo()).isEqualTo("SLTDA-12345");
	}

	@Test
	void findByUser_Id_nonExistingUser_returnsEmpty() {
		Optional<Guide> found = guideRepository.findByUser_Id(java.util.UUID.randomUUID());

		assertThat(found).isEmpty();
	}

	@Test
	void save_nullFields_createsProfileWithDefaults() {
		Guide guide = guideRepository.save(Guide.builder()
				.user(savedUser)
				.build());

		assertThat(guide.getGuideId()).isNotNull();
		assertThat(guide.getBio()).isNull();
		assertThat(guide.getSpecializations()).isNull();
		assertThat(guide.getLicenseNo()).isNull();
		assertThat(guide.getRating()).isEqualTo(0.0);
	}
}
