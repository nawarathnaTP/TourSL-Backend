package com.tourplanner.planning.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourplanner.planning.dto.AuthResponse;
import com.tourplanner.planning.dto.LoginRequest;
import com.tourplanner.planning.dto.RefreshTokenRequest;
import com.tourplanner.planning.dto.RegisterRequest;
import com.tourplanner.planning.exception.GlobalExceptionHandler;
import com.tourplanner.planning.security.JwtUtil;
import com.tourplanner.planning.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private JwtUtil jwtUtil;

	@MockitoBean
	private UserDetailsService userDetailsService;

	private AuthResponse buildAuthResponse() {
		return AuthResponse.builder()
				.accessToken("access-token")
				.refreshToken("refresh-token")
				.email("john@example.com")
				.firstName("John")
				.lastName("Doe")
				.build();
	}

	@Test
	void register_validRequest_returns200() throws Exception {
		RegisterRequest request = new RegisterRequest();
		request.setFirstName("John");
		request.setLastName("Doe");
		request.setEmail("john@example.com");
		request.setPassword("password123");

		when(authService.register(any(RegisterRequest.class))).thenReturn(buildAuthResponse());

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
				.andExpect(jsonPath("$.email").value("john@example.com"))
				.andExpect(jsonPath("$.firstName").value("John"));
	}

	@Test
	void register_missingFields_returns400() throws Exception {
		RegisterRequest request = new RegisterRequest();

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void register_invalidEmail_returns400() throws Exception {
		RegisterRequest request = new RegisterRequest();
		request.setFirstName("John");
		request.setLastName("Doe");
		request.setEmail("not-an-email");
		request.setPassword("password123");

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void register_shortPassword_returns400() throws Exception {
		RegisterRequest request = new RegisterRequest();
		request.setFirstName("John");
		request.setLastName("Doe");
		request.setEmail("john@example.com");
		request.setPassword("short");

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void login_validRequest_returns200() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setEmail("john@example.com");
		request.setPassword("password123");

		when(authService.login(any(LoginRequest.class))).thenReturn(buildAuthResponse());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.email").value("john@example.com"));
	}

	@Test
	void login_missingEmail_returns400() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setPassword("password123");

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void login_invalidCredentials_returns400() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setEmail("john@example.com");
		request.setPassword("wrong-password");

		when(authService.login(any(LoginRequest.class)))
				.thenThrow(new IllegalArgumentException("Invalid email or password"));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Invalid email or password"));
	}

	@Test
	void refreshToken_validRequest_returns200() throws Exception {
		RefreshTokenRequest request = new RefreshTokenRequest();
		request.setRefreshToken("valid-refresh-token");

		when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(buildAuthResponse());

		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"));
	}

	@Test
	void refreshToken_missingToken_returns400() throws Exception {
		RefreshTokenRequest request = new RefreshTokenRequest();

		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}
}
