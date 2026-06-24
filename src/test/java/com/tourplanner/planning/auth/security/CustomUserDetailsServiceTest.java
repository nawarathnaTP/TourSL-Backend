package com.tourplanner.planning.auth.security;

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
	void loadUserByUsername_existingUser_returnsUserDetails() {
		User user = User.builder()
				.id(1L)
				.firstName("John")
				.lastName("Doe")
				.email("john@example.com")
				.build();

		when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

		UserDetails userDetails = customUserDetailsService.loadUserByUsername("john@example.com");

		assertThat(userDetails.getUsername()).isEqualTo("john@example.com");
		assertThat(userDetails.getAuthorities()).isEmpty();
	}

	@Test
	void loadUserByUsername_nonExistingUser_throwsException() {
		when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("nobody@example.com"))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("nobody@example.com");
	}
}
