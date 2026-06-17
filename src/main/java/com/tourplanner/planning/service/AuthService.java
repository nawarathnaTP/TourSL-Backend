package com.tourplanner.planning.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.tourplanner.planning.dto.*;
import com.tourplanner.planning.entity.Account;
import com.tourplanner.planning.entity.AuthProvider;
import com.tourplanner.planning.entity.User;
import com.tourplanner.planning.repository.AccountRepository;
import com.tourplanner.planning.repository.UserRepository;
import com.tourplanner.planning.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final AccountRepository accountRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;

	@Value("${google.oauth.client-id}")
	private String googleClientId;

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new IllegalArgumentException("Email is already registered");
		}

		User user = User.builder()
				.firstName(request.getFirstName())
				.lastName(request.getLastName())
				.email(request.getEmail())
				.build();
		userRepository.save(user);

		Account account = Account.builder()
				.user(user)
				.authProvider(AuthProvider.LOCAL)
				.password(passwordEncoder.encode(request.getPassword()))
				.build();
		accountRepository.save(account);

		return buildAuthResponse(user);
	}

	public AuthResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

		Account account = accountRepository.findByUserAndAuthProvider(user, AuthProvider.LOCAL)
				.orElseThrow(() -> new IllegalArgumentException("No local account found. Try signing in with Google."));

		if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
			throw new IllegalArgumentException("Invalid email or password");
		}

		return buildAuthResponse(user);
	}

	@Transactional
	public AuthResponse googleAuth(GoogleAuthRequest request) {
		GoogleIdToken.Payload payload = verifyGoogleToken(request.getIdToken());

		String googleId = payload.getSubject();
		String email = payload.getEmail();
		String firstName = (String) payload.get("given_name");
		String lastName = (String) payload.get("family_name");
		String pictureUrl = (String) payload.get("picture");

		// Check if a Google account already exists with this googleId
		var existingAccount = accountRepository.findByGoogleId(googleId);
		if (existingAccount.isPresent()) {
			return buildAuthResponse(existingAccount.get().getUser());
		}

		// Check if user exists by email (may have a local account)
		User user = userRepository.findByEmail(email).orElse(null);

		if (user == null) {
			user = User.builder()
					.firstName(firstName != null ? firstName : "")
					.lastName(lastName != null ? lastName : "")
					.email(email)
					.profilePictureUrl(pictureUrl)
					.build();
			userRepository.save(user);
		}

		Account account = Account.builder()
				.user(user)
				.authProvider(AuthProvider.GOOGLE)
				.googleId(googleId)
				.build();
		accountRepository.save(account);

		return buildAuthResponse(user);
	}

	public AuthResponse refreshToken(RefreshTokenRequest request) {
		String token = request.getRefreshToken();

		if (!jwtUtil.isTokenValid(token) || !"refresh".equals(jwtUtil.extractTokenType(token))) {
			throw new IllegalArgumentException("Invalid refresh token");
		}

		String email = jwtUtil.extractEmail(token);
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		return buildAuthResponse(user);
	}

	private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
		try {
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
					new NetHttpTransport(), GsonFactory.getDefaultInstance())
					.setAudience(Collections.singletonList(googleClientId))
					.build();

			GoogleIdToken idToken = verifier.verify(idTokenString);
			if (idToken == null) {
				throw new IllegalArgumentException("Invalid Google ID token");
			}
			return idToken.getPayload();
		} catch (GeneralSecurityException | IOException e) {
			throw new IllegalArgumentException("Failed to verify Google ID token", e);
		}
	}

	private AuthResponse buildAuthResponse(User user) {
		return AuthResponse.builder()
				.accessToken(jwtUtil.generateAccessToken(user.getEmail()))
				.refreshToken(jwtUtil.generateRefreshToken(user.getEmail()))
				.email(user.getEmail())
				.firstName(user.getFirstName())
				.lastName(user.getLastName())
				.build();
	}
}
