package com.tourplanner.planning.auth.service;

import com.tourplanner.planning.auth.dto.AuthResponse;
import com.tourplanner.planning.auth.dto.LoginRequest;
import com.tourplanner.planning.auth.dto.RefreshTokenRequest;
import com.tourplanner.planning.auth.dto.RegisterRequest;
import com.tourplanner.planning.auth.entity.Account;
import com.tourplanner.planning.auth.entity.AuthProvider;
import com.tourplanner.planning.auth.entity.User;
import com.tourplanner.planning.auth.repository.AccountRepository;
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
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtUtil jwtUtil;

	@InjectMocks
	private AuthService authService;

	private User testUser;

	@BeforeEach
	void setUp() {
		testUser = User.builder()
				.id(UUID.randomUUID())
				.firstName("John")
				.lastName("Doe")
				.email("john@example.com")
				.build();
	}

	@Test
	void register_newUser_returnsAuthResponse() {
		RegisterRequest request = new RegisterRequest();
		request.setFirstName("John");
		request.setLastName("Doe");
		request.setEmail("john@example.com");
		request.setPassword("password123");

		when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenReturn(testUser);
		when(accountRepository.save(any(Account.class))).thenReturn(Account.builder().build());
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(jwtUtil.generateAccessToken("john@example.com")).thenReturn("access-token");
		when(jwtUtil.generateRefreshToken("john@example.com")).thenReturn("refresh-token");

		AuthResponse response = authService.register(request);

		assertThat(response.getAccessToken()).isEqualTo("access-token");
		assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
		assertThat(response.getEmail()).isEqualTo("john@example.com");
		assertThat(response.getFirstName()).isEqualTo("John");
		assertThat(response.getLastName()).isEqualTo("Doe");

		verify(userRepository).save(any(User.class));
		verify(accountRepository).save(any(Account.class));
		verify(passwordEncoder).encode("password123");
	}

	@Test
	void register_existingEmail_throwsException() {
		RegisterRequest request = new RegisterRequest();
		request.setEmail("john@example.com");

		when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Email is already registered");

		verify(userRepository, never()).save(any());
	}

	@Test
	void login_validCredentials_returnsAuthResponse() {
		LoginRequest request = new LoginRequest();
		request.setEmail("john@example.com");
		request.setPassword("password123");

		Account account = Account.builder()
				.user(testUser)
				.authProvider(AuthProvider.LOCAL)
				.password("encoded-password")
				.build();

		when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
		when(accountRepository.findByUserAndAuthProvider(testUser, AuthProvider.LOCAL))
				.thenReturn(Optional.of(account));
		when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
		when(jwtUtil.generateAccessToken("john@example.com")).thenReturn("access-token");
		when(jwtUtil.generateRefreshToken("john@example.com")).thenReturn("refresh-token");

		AuthResponse response = authService.login(request);

		assertThat(response.getAccessToken()).isEqualTo("access-token");
		assertThat(response.getEmail()).isEqualTo("john@example.com");
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

		when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
		when(accountRepository.findByUserAndAuthProvider(testUser, AuthProvider.LOCAL))
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
				.user(testUser)
				.authProvider(AuthProvider.LOCAL)
				.password("encoded-password")
				.build();

		when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
		when(accountRepository.findByUserAndAuthProvider(testUser, AuthProvider.LOCAL))
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
		when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
		when(jwtUtil.generateAccessToken("john@example.com")).thenReturn("new-access-token");
		when(jwtUtil.generateRefreshToken("john@example.com")).thenReturn("new-refresh-token");

		AuthResponse response = authService.refreshToken(request);

		assertThat(response.getAccessToken()).isEqualTo("new-access-token");
		assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
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
