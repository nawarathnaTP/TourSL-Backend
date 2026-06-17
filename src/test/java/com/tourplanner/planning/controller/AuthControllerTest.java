package com.tourplanner.planning.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourplanner.planning.dto.LoginRequest;
import com.tourplanner.planning.dto.RefreshTokenRequest;
import com.tourplanner.planning.dto.RegisterRequest;
import com.tourplanner.planning.repository.AccountRepository;
import com.tourplanner.planning.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AccountRepository accountRepository;

	private String refreshToken;

	private static final String TEST_EMAIL = "testuser@example.com";
	private static final String TEST_PASSWORD = "password123";

	@BeforeAll
	void cleanup() {
		userRepository.findByEmail(TEST_EMAIL).ifPresent(user -> {
			accountRepository.deleteAll(user.getAccounts());
			userRepository.delete(user);
		});
	}

	@AfterAll
	void teardown() {
		userRepository.findByEmail(TEST_EMAIL).ifPresent(user -> {
			accountRepository.deleteAll(user.getAccounts());
			userRepository.delete(user);
		});
	}

	@Test
	@Order(1)
	void register_shouldCreateUserAndReturnTokens() throws Exception {
		RegisterRequest request = new RegisterRequest();
		request.setFirstName("Test");
		request.setLastName("User");
		request.setEmail(TEST_EMAIL);
		request.setPassword(TEST_PASSWORD);

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists())
				.andExpect(jsonPath("$.refreshToken").exists())
				.andExpect(jsonPath("$.email").value(TEST_EMAIL))
				.andExpect(jsonPath("$.firstName").value("Test"))
				.andExpect(jsonPath("$.lastName").value("User"));
	}

	@Test
	@Order(2)
	void register_duplicateEmail_shouldFail() throws Exception {
		RegisterRequest request = new RegisterRequest();
		request.setFirstName("Test");
		request.setLastName("User");
		request.setEmail(TEST_EMAIL);
		request.setPassword(TEST_PASSWORD);

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@Order(3)
	void login_validCredentials_shouldReturnTokens() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setEmail(TEST_EMAIL);
		request.setPassword(TEST_PASSWORD);

		MvcResult result = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists())
				.andExpect(jsonPath("$.refreshToken").exists())
				.andExpect(jsonPath("$.email").value(TEST_EMAIL))
				.andReturn();

		JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
		refreshToken = json.get("refreshToken").asText();
	}

	@Test
	@Order(4)
	void login_wrongPassword_shouldFail() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setEmail(TEST_EMAIL);
		request.setPassword("wrongpassword");

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@Order(5)
	void login_nonExistentEmail_shouldFail() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setEmail("nobody@example.com");
		request.setPassword(TEST_PASSWORD);

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@Order(6)
	void refreshToken_validToken_shouldReturnNewTokens() throws Exception {
		RefreshTokenRequest request = new RefreshTokenRequest();
		request.setRefreshToken(refreshToken);

		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists())
				.andExpect(jsonPath("$.refreshToken").exists())
				.andExpect(jsonPath("$.email").value(TEST_EMAIL));
	}

	@Test
	@Order(7)
	void refreshToken_invalidToken_shouldFail() throws Exception {
		RefreshTokenRequest request = new RefreshTokenRequest();
		request.setRefreshToken("invalid.token.here");

		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@Order(8)
	void accessProtectedEndpoint_withToken_shouldSucceed() throws Exception {
		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setEmail(TEST_EMAIL);
		loginRequest.setPassword(TEST_PASSWORD);

		MvcResult result = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(loginRequest)))
				.andReturn();

		JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
		String accessToken = json.get("accessToken").asText();

		mockMvc.perform(get("/actuator/health")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk());
	}

	@Test
	@Order(9)
	void accessProtectedEndpoint_withoutToken_shouldBeUnauthorized() throws Exception {
		mockMvc.perform(get("/api/some-protected-resource"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@Order(10)
	void register_invalidEmail_shouldFail() throws Exception {
		RegisterRequest request = new RegisterRequest();
		request.setFirstName("Test");
		request.setLastName("User");
		request.setEmail("not-an-email");
		request.setPassword(TEST_PASSWORD);

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@Order(11)
	void register_shortPassword_shouldFail() throws Exception {
		RegisterRequest request = new RegisterRequest();
		request.setFirstName("Test");
		request.setLastName("User");
		request.setEmail("another@example.com");
		request.setPassword("short");

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}
}
