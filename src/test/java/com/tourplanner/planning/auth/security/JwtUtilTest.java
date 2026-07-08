package com.tourplanner.planning.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

	private JwtUtil jwtUtil;

	@BeforeEach
	void setUp() {
		jwtUtil = new JwtUtil(
				"test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha",
				86400000L,
				604800000L
		);
	}

	@Test
	void generateAccessToken_returnsValidToken() {
		String token = jwtUtil.generateAccessToken("user@example.com", "TOURIST");

		assertThat(token).isNotBlank();
		assertThat(jwtUtil.isTokenValid(token)).isTrue();
	}

	@Test
	void generateRefreshToken_returnsValidToken() {
		String token = jwtUtil.generateRefreshToken("user@example.com", "TOURIST");

		assertThat(token).isNotBlank();
		assertThat(jwtUtil.isTokenValid(token)).isTrue();
	}

	@Test
	void extractEmail_returnsCorrectEmail() {
		String token = jwtUtil.generateAccessToken("user@example.com", "TOURIST");

		assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@example.com");
	}

	@Test
	void extractTokenType_accessToken_returnsAccess() {
		String token = jwtUtil.generateAccessToken("user@example.com", "TOURIST");

		assertThat(jwtUtil.extractTokenType(token)).isEqualTo("access");
	}

	@Test
	void extractTokenType_refreshToken_returnsRefresh() {
		String token = jwtUtil.generateRefreshToken("user@example.com", "TOURIST");

		assertThat(jwtUtil.extractTokenType(token)).isEqualTo("refresh");
	}

	@Test
	void extractRole_touristToken_returnsTourist() {
		String token = jwtUtil.generateAccessToken("user@example.com", "TOURIST");

		assertThat(jwtUtil.extractRole(token)).isEqualTo("TOURIST");
	}

	@Test
	void extractRole_guideToken_returnsGuide() {
		String token = jwtUtil.generateAccessToken("guide@example.com", "GUIDE");

		assertThat(jwtUtil.extractRole(token)).isEqualTo("GUIDE");
	}

	@Test
	void extractRole_refreshToken_returnsRole() {
		String token = jwtUtil.generateRefreshToken("user@example.com", "GUIDE");

		assertThat(jwtUtil.extractRole(token)).isEqualTo("GUIDE");
	}

	@Test
	void isTokenValid_validToken_returnsTrue() {
		String token = jwtUtil.generateAccessToken("user@example.com", "TOURIST");

		assertThat(jwtUtil.isTokenValid(token)).isTrue();
	}

	@Test
	void isTokenValid_invalidToken_returnsFalse() {
		assertThat(jwtUtil.isTokenValid("invalid-token")).isFalse();
	}

	@Test
	void isTokenValid_expiredToken_returnsFalse() {
		JwtUtil shortLivedJwtUtil = new JwtUtil(
				"test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha",
				-1000L,
				-1000L
		);
		String token = shortLivedJwtUtil.generateAccessToken("user@example.com", "TOURIST");

		assertThat(jwtUtil.isTokenValid(token)).isFalse();
	}

	@Test
	void isTokenValid_tamperedToken_returnsFalse() {
		String token = jwtUtil.generateAccessToken("user@example.com", "TOURIST");
		String tampered = token.substring(0, token.length() - 5) + "XXXXX";

		assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
	}
}
