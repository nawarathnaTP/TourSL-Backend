package com.tourplanner.planning.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtUtil jwtUtil;

	@Mock
	private UserDetailsService userDetailsService;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private FilterChain filterChain;

	@InjectMocks
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void doFilterInternal_noAuthHeader_continuesFilterChain() throws Exception {
		when(request.getHeader("Authorization")).thenReturn(null);

		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void doFilterInternal_nonBearerHeader_continuesFilterChain() throws Exception {
		when(request.getHeader("Authorization")).thenReturn("Basic some-token");

		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void doFilterInternal_invalidToken_continuesWithoutAuth() throws Exception {
		when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
		when(jwtUtil.isTokenValid("invalid-token")).thenReturn(false);

		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void doFilterInternal_refreshTokenUsed_continuesWithoutAuth() throws Exception {
		when(request.getHeader("Authorization")).thenReturn("Bearer refresh-token");
		when(jwtUtil.isTokenValid("refresh-token")).thenReturn(true);
		when(jwtUtil.extractTokenType("refresh-token")).thenReturn("refresh");

		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void doFilterInternal_validAccessToken_setsAuthentication() throws Exception {
		when(request.getHeader("Authorization")).thenReturn("Bearer valid-access-token");
		when(jwtUtil.isTokenValid("valid-access-token")).thenReturn(true);
		when(jwtUtil.extractTokenType("valid-access-token")).thenReturn("access");
		when(jwtUtil.extractEmail("valid-access-token")).thenReturn("user@example.com");

		UserDetails userDetails = new User("user@example.com", "", Collections.emptyList());
		when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);

		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user@example.com");
	}
}
