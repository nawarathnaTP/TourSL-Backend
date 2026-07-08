package com.tourplanner.planning.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "guide")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Guide {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "guide_id")
	private UUID guideId;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	private String bio;

	private String specializations;

	@Column(name = "license_no")
	private String licenseNo;

	@Builder.Default
	private Double rating = 0.0;
}
