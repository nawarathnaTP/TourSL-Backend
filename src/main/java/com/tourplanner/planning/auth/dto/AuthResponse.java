package com.tourplanner.planning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {

	private String accessToken;
	private String refreshToken;
	private String email;
	private String firstName;
	private String lastName;
}
