package com.tourplanner.planning.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourplanner.planning.auth.dto.*;
import com.tourplanner.planning.auth.exception.GlobalExceptionHandler;
import com.tourplanner.planning.auth.security.JwtUtil;
import com.tourplanner.planning.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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

	private AuthResponse buildTouristAuthResponse() {
		return AuthResponse.builder()
				.accessToken("access-token")
				.refreshToken("refresh-token")
				.email("john@example.com")
				.firstName("John")
				.lastName("Doe")
				.role("TOURIST")
				.build();
	}

	private AuthResponse buildGuideAuthResponse() {
		return AuthResponse.builder()
				.accessToken("access-token")
				.refreshToken("refresh-token")
				.email("kamal@example.com")
				.firstName("Kamal")
				.lastName("Perera")
				.role("GUIDE")
				.build();
	}

	private RegisterRequest buildValidRegisterRequest(String email) {
		RegisterRequest request = new RegisterRequest();
		request.setFirstName("John");
		request.setLastName("Doe");
		request.setEmail(email);
		request.setPassword("password123");
		return request;
	}

	// Tourist registration tests

	@Test
	void registerTourist_validRequest_returns200WithTouristRole() throws Exception {
		when(authService.registerTourist(any(RegisterRequest.class))).thenReturn(buildTouristAuthResponse());

		mockMvc.perform(post("/api/auth/tourist/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(buildValidRegisterRequest("john@example.com"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.role").value("TOURIST"));
	}

	@Test
	void registerTourist_missingFields_returns400() throws Exception {
		mockMvc.perform(post("/api/auth/tourist/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new RegisterRequest())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void registerTourist_invalidEmail_returns400() throws Exception {
		RegisterRequest request = buildValidRegisterRequest("not-an-email");
		request.setEmail("not-an-email");

		mockMvc.perform(post("/api/auth/tourist/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void registerTourist_shortPassword_returns400() throws Exception {
		RegisterRequest request = buildValidRegisterRequest("john@example.com");
		request.setPassword("short");

		mockMvc.perform(post("/api/auth/tourist/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	// Guide registration tests

	@Test
	void registerGuide_validRequest_returns200WithGuideRole() throws Exception {
		when(authService.registerGuide(any(RegisterRequest.class))).thenReturn(buildGuideAuthResponse());

		mockMvc.perform(post("/api/auth/guide/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(buildValidRegisterRequest("kamal@example.com"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.role").value("GUIDE"));
	}

	@Test
	void registerGuide_missingFields_returns400() throws Exception {
		mockMvc.perform(post("/api/auth/guide/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new RegisterRequest())))
				.andExpect(status().isBadRequest());
	}

	// Google auth tests

	@Test
	void googleAuthTourist_validRequest_returns200() throws Exception {
		GoogleAuthRequest request = new GoogleAuthRequest();
		request.setIdToken("valid-google-token");

		when(authService.googleAuthTourist(any(GoogleAuthRequest.class))).thenReturn(buildTouristAuthResponse());

		mockMvc.perform(post("/api/auth/tourist/google")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("TOURIST"));
	}

	@Test
	void googleAuthGuide_validRequest_returns200() throws Exception {
		GoogleAuthRequest request = new GoogleAuthRequest();
		request.setIdToken("valid-google-token");

		when(authService.googleAuthGuide(any(GoogleAuthRequest.class))).thenReturn(buildGuideAuthResponse());

		mockMvc.perform(post("/api/auth/guide/google")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("GUIDE"));
	}

	// Login tests

	@Test
	void login_validRequest_returns200() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setEmail("john@example.com");
		request.setPassword("password123");

		when(authService.login(any(LoginRequest.class))).thenReturn(buildTouristAuthResponse());

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

	// Refresh token tests

	@Test
	void refreshToken_validRequest_returns200() throws Exception {
		RefreshTokenRequest request = new RefreshTokenRequest();
		request.setRefreshToken("valid-refresh-token");

		when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(buildTouristAuthResponse());

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
