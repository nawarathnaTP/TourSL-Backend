package com.tourplanner.planning.auth.controller;

import com.tourplanner.planning.auth.dto.*;
import com.tourplanner.planning.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/tourist/register")
	public ResponseEntity<AuthResponse> registerTourist(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.ok(authService.registerTourist(request));
	}

	@PostMapping("/tourist/google")
	public ResponseEntity<AuthResponse> googleAuthTourist(@Valid @RequestBody GoogleAuthRequest request) {
		return ResponseEntity.ok(authService.googleAuthTourist(request));
	}

	@PostMapping("/guide/register")
	public ResponseEntity<AuthResponse> registerGuide(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.ok(authService.registerGuide(request));
	}

	@PostMapping("/guide/google")
	public ResponseEntity<AuthResponse> googleAuthGuide(@Valid @RequestBody GoogleAuthRequest request) {
		return ResponseEntity.ok(authService.googleAuthGuide(request));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
		return ResponseEntity.ok(authService.refreshToken(request));
	}
}
