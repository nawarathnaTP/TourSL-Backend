package com.tourplanner.planning.auth.service;

import com.tourplanner.planning.auth.dto.AuthResponse;
import com.tourplanner.planning.auth.dto.LoginRequest;
import com.tourplanner.planning.auth.dto.RefreshTokenRequest;
import com.tourplanner.planning.auth.dto.RegisterRequest;
import com.tourplanner.planning.auth.entity.*;
import com.tourplanner.planning.auth.repository.AccountRepository;
import com.tourplanner.planning.auth.repository.GuideRepository;
import com.tourplanner.planning.auth.repository.TouristRepository;
import com.tourplanner.planning.auth.repository.UserRepository;
import com.tourplanner.planning.auth.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private TouristRepository touristRepository;

	@Mock
	private GuideRepository guideRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtUtil jwtUtil;

	@InjectMocks
	private AuthServiceImpl authService;

	private User testTourist;
	private User testGuide;

	@BeforeEach
	void setUp() {
		testTourist = User.builder()
				.id(UUID.randomUUID())
				.firstName("John")
				.lastName("Doe")
				.email("john@example.com")
				.role(Role.TOURIST)
				.build();

		testGuide = User.builder()
				.id(UUID.randomUUID())
				.firstName("Kamal")
				.lastName("Perera")
				.email("kamal@example.com")
				.role(Role.GUIDE)
				.build();
	}

	@Test
	void registerTourist_newUser_returnsAuthResponseWithTouristRole() {
		RegisterRequest request = new RegisterRequest();
		request.setFirstName("John");
		request.setLastName("Doe");
		request.setEmail("john@example.com");
		request.setPassword("password123");

		when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenReturn(testTourist);
		when(accountRepository.save(any(Account.class))).thenReturn(Account.builder().build());
		when(touristRepository.save(any(Tourist.class))).thenReturn(Tourist.builder().build());
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(jwtUtil.generateAccessToken("john@example.com", "TOURIST")).thenReturn("access-token");
		when(jwtUtil.generateRefreshToken("john@example.com", "TOURIST")).thenReturn("refresh-token");

		AuthResponse response = authService.registerTourist(request);

		assertThat(response.getAccessToken()).isEqualTo("access-token");
		assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
		assertThat(response.getEmail()).isEqualTo("john@example.com");
		assertThat(response.getRole()).isEqualTo("TOURIST");

		verify(userRepository).save(any(User.class));
		verify(accountRepository).save(any(Account.class));
		verify(touristRepository).save(any(Tourist.class));
		verify(guideRepository, never()).save(any());
	}

	@Test
	void registerGuide_newUser_returnsAuthResponseWithGuideRole() {
		RegisterRequest request = new RegisterRequest();
		request.setFirstName("Kamal");
		request.setLastName("Perera");
		request.setEmail("kamal@example.com");
		request.setPassword("password123");

		when(userRepository.existsByEmail("kamal@example.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenReturn(testGuide);
		when(accountRepository.save(any(Account.class))).thenReturn(Account.builder().build());
		when(guideRepository.save(any(Guide.class))).thenReturn(Guide.builder().build());
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(jwtUtil.generateAccessToken("kamal@example.com", "GUIDE")).thenReturn("access-token");
		when(jwtUtil.generateRefreshToken("kamal@example.com", "GUIDE")).thenReturn("refresh-token");

		AuthResponse response = authService.registerGuide(request);

		assertThat(response.getAccessToken()).isEqualTo("access-token");
		assertThat(response.getRole()).isEqualTo("GUIDE");

		verify(guideRepository).save(any(Guide.class));
		verify(touristRepository, never()).save(any());
	}

	@Test
	void registerTourist_existingEmail_throwsException() {
		RegisterRequest request = new RegisterRequest();
		request.setEmail("john@example.com");

		when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.registerTourist(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Email is already registered");

		verify(userRepository, never()).save(any());
	}

	@Test
	void registerGuide_existingEmail_throwsException() {
		RegisterRequest request = new RegisterRequest();
		request.setEmail("kamal@example.com");

		when(userRepository.existsByEmail("kamal@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.registerGuide(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Email is already registered");

		verify(userRepository, never()).save(any());
	}

	@Test
	void login_validTouristCredentials_returnsAuthResponse() {
		LoginRequest request = new LoginRequest();
		request.setEmail("john@example.com");
		request.setPassword("password123");

		Account account = Account.builder()
				.user(testTourist)
				.authProvider(AuthProvider.LOCAL)
				.password("encoded-password")
				.build();

		when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testTourist));
		when(accountRepository.findByUserAndAuthProvider(testTourist, AuthProvider.LOCAL))
				.thenReturn(Optional.of(account));
		when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
		when(jwtUtil.generateAccessToken("john@example.com", "TOURIST")).thenReturn("access-token");
		when(jwtUtil.generateRefreshToken("john@example.com", "TOURIST")).thenReturn("refresh-token");

		AuthResponse response = authService.login(request);

		assertThat(response.getAccessToken()).isEqualTo("access-token");
		assertThat(response.getEmail()).isEqualTo("john@example.com");
		assertThat(response.getRole()).isEqualTo("TOURIST");
	}

	@Test
	void login_validGuideCredentials_returnsAuthResponseWithGuideRole() {
		LoginRequest request = new LoginRequest();
		request.setEmail("kamal@example.com");
		request.setPassword("password123");

		Account account = Account.builder()
				.user(testGuide)
				.authProvider(AuthProvider.LOCAL)
				.password("encoded-password")
				.build();

		when(userRepository.findByEmail("kamal@example.com")).thenReturn(Optional.of(testGuide));
		when(accountRepository.findByUserAndAuthProvider(testGuide, AuthProvider.LOCAL))
				.thenReturn(Optional.of(account));
		when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
		when(jwtUtil.generateAccessToken("kamal@example.com", "GUIDE")).thenReturn("access-token");
		when(jwtUtil.generateRefreshToken("kamal@example.com", "GUIDE")).thenReturn("refresh-token");

		AuthResponse response = authService.login(request);

		assertThat(response.getRole()).isEqualTo("GUIDE");
	}

	@Test
	void login_invalidEmail_throwsException() {
		LoginRequest request = new LoginRequest();
		request.setEmail("nobody@example.com");
		request.setPassword("password123");

		when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Invalid email or password");
	}

	@Test
	void login_noLocalAccount_throwsException() {
		LoginRequest request = new LoginRequest();
		request.setEmail("john@example.com");
		request.setPassword("password123");

		when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testTourist));
		when(accountRepository.findByUserAndAuthProvider(testTourist, AuthProvider.LOCAL))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("No local account found. Try signing in with Google.");
	}

	@Test
	void login_wrongPassword_throwsException() {
		LoginRequest request = new LoginRequest();
		request.setEmail("john@example.com");
		request.setPassword("wrong-password");

		Account account = Account.builder()
				.user(testTourist)
				.authProvider(AuthProvider.LOCAL)
				.password("encoded-password")
				.build();

		when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testTourist));
		when(accountRepository.findByUserAndAuthProvider(testTourist, AuthProvider.LOCAL))
				.thenReturn(Optional.of(account));
		when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Invalid email or password");
	}

	@Test
	void refreshToken_validRefreshToken_returnsAuthResponse() {
		RefreshTokenRequest request = new RefreshTokenRequest();
		request.setRefreshToken("valid-refresh-token");

		when(jwtUtil.isTokenValid("valid-refresh-token")).thenReturn(true);
		when(jwtUtil.extractTokenType("valid-refresh-token")).thenReturn("refresh");
		when(jwtUtil.extractEmail("valid-refresh-token")).thenReturn("john@example.com");
		when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testTourist));
		when(jwtUtil.generateAccessToken("john@example.com", "TOURIST")).thenReturn("new-access-token");
		when(jwtUtil.generateRefreshToken("john@example.com", "TOURIST")).thenReturn("new-refresh-token");

		AuthResponse response = authService.refreshToken(request);

		assertThat(response.getAccessToken()).isEqualTo("new-access-token");
		assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
		assertThat(response.getRole()).isEqualTo("TOURIST");
	}

	@Test
	void refreshToken_invalidToken_throwsException() {
		RefreshTokenRequest request = new RefreshTokenRequest();
		request.setRefreshToken("invalid-token");

		when(jwtUtil.isTokenValid("invalid-token")).thenReturn(false);

		assertThatThrownBy(() -> authService.refreshToken(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Invalid refresh token");
	}

	@Test
	void refreshToken_accessTokenUsedAsRefresh_throwsException() {
		RefreshTokenRequest request = new RefreshTokenRequest();
		request.setRefreshToken("access-token");

		when(jwtUtil.isTokenValid("access-token")).thenReturn(true);
		when(jwtUtil.extractTokenType("access-token")).thenReturn("access");

		assertThatThrownBy(() -> authService.refreshToken(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Invalid refresh token");
	}

	@Test
	void refreshToken_userNotFound_throwsException() {
		RefreshTokenRequest request = new RefreshTokenRequest();
		request.setRefreshToken("valid-refresh-token");

		when(jwtUtil.isTokenValid("valid-refresh-token")).thenReturn(true);
		when(jwtUtil.extractTokenType("valid-refresh-token")).thenReturn("refresh");
		when(jwtUtil.extractEmail("valid-refresh-token")).thenReturn("deleted@example.com");
		when(userRepository.findByEmail("deleted@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.refreshToken(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("User not found");
	}
}
