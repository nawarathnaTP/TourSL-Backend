package com.tourplanner.planning.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleAuthRequest {

	@NotBlank
	private String idToken;
}
