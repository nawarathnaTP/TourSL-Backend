package com.tourplanner.planning.auth.service;

import com.tourplanner.planning.auth.dto.*;

public interface AuthService {

	AuthResponse registerTourist(RegisterRequest request);

	AuthResponse registerGuide(RegisterRequest request);

	AuthResponse login(LoginRequest request);

	AuthResponse googleAuthTourist(GoogleAuthRequest request);

	AuthResponse googleAuthGuide(GoogleAuthRequest request);

	AuthResponse refreshToken(RefreshTokenRequest request);
}
