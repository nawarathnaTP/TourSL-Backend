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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtUtil jwtUtil;

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
	void doFilterInternal_validAccessToken_setsAuthenticationWithRole() throws Exception {
		when(request.getHeader("Authorization")).thenReturn("Bearer valid-access-token");
		when(jwtUtil.isTokenValid("valid-access-token")).thenReturn(true);
		when(jwtUtil.extractTokenType("valid-access-token")).thenReturn("access");
		when(jwtUtil.extractEmail("valid-access-token")).thenReturn("user@example.com");
		when(jwtUtil.extractRole("valid-access-token")).thenReturn("TOURIST");

		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		var auth = SecurityContextHolder.getContext().getAuthentication();
		assertThat(auth).isNotNull();
		assertThat(auth.getName()).isEqualTo("user@example.com");
		assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_TOURIST"));
	}

	@Test
	void doFilterInternal_validGuideToken_setsGuideRole() throws Exception {
		when(request.getHeader("Authorization")).thenReturn("Bearer guide-token");
		when(jwtUtil.isTokenValid("guide-token")).thenReturn(true);
		when(jwtUtil.extractTokenType("guide-token")).thenReturn("access");
		when(jwtUtil.extractEmail("guide-token")).thenReturn("guide@example.com");
		when(jwtUtil.extractRole("guide-token")).thenReturn("GUIDE");

		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		var auth = SecurityContextHolder.getContext().getAuthentication();
		assertThat(auth).isNotNull();
		assertThat(auth.getName()).isEqualTo("guide@example.com");
		assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_GUIDE"));
	}
}
