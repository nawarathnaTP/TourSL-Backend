package com.tourplanner.planning.auth.security;

import com.tourplanner.planning.auth.entity.Role;
import com.tourplanner.planning.auth.entity.User;
import com.tourplanner.planning.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private CustomUserDetailsService customUserDetailsService;

	@Test
	void loadUserByUsername_existingTourist_returnsUserDetailsWithTouristRole() {
		User user = User.builder()
				.id(UUID.randomUUID())
				.firstName("John")
				.lastName("Doe")
				.email("john@example.com")
				.role(Role.TOURIST)
				.build();

		when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

		UserDetails userDetails = customUserDetailsService.loadUserByUsername("john@example.com");

		assertThat(userDetails.getUsername()).isEqualTo("john@example.com");
		assertThat(userDetails.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_TOURIST"));
	}

	@Test
	void loadUserByUsername_existingGuide_returnsUserDetailsWithGuideRole() {
		User user = User.builder()
				.id(UUID.randomUUID())
				.firstName("Kamal")
				.lastName("Perera")
				.email("kamal@example.com")
				.role(Role.GUIDE)
				.build();

		when(userRepository.findByEmail("kamal@example.com")).thenReturn(Optional.of(user));

		UserDetails userDetails = customUserDetailsService.loadUserByUsername("kamal@example.com");

		assertThat(userDetails.getUsername()).isEqualTo("kamal@example.com");
		assertThat(userDetails.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_GUIDE"));
	}

	@Test
	void loadUserByUsername_nonExistingUser_throwsException() {
		when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("nobody@example.com"))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("nobody@example.com");
	}
}
