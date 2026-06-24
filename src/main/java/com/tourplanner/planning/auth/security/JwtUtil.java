package com.tourplanner.planning.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

	private final SecretKey key;
	private final long accessTokenExpiration;
	private final long refreshTokenExpiration;

	public JwtUtil(
			@Value("${JWT_SECRET}") String secret,
			@Value("${jwt.expiration}") long accessTokenExpiration,
			@Value("${jwt.refresh-expiration}") long refreshTokenExpiration) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenExpiration = accessTokenExpiration;
		this.refreshTokenExpiration = refreshTokenExpiration;
	}

	public String generateAccessToken(String email) {
		return buildToken(email, accessTokenExpiration, "access");
	}

	public String generateRefreshToken(String email) {
		return buildToken(email, refreshTokenExpiration, "refresh");
	}

	private String buildToken(String email, long expiration, String type) {
		return Jwts.builder()
				.subject(email)
				.claim("type", type)
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + expiration))
				.signWith(key)
				.compact();
	}

	public String extractEmail(String token) {
		return extractClaims(token).getSubject();
	}

	public String extractTokenType(String token) {
		return extractClaims(token).get("type", String.class);
	}

	public boolean isTokenValid(String token) {
		try {
			extractClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private Claims extractClaims(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
