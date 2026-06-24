package com.tourplanner.planning.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long id;

	@Column(nullable = false, name="first_name")
	private String firstName;

	@Column(nullable = false, name="last_name")
	private String lastName;

	@Column(nullable = false, unique = true)
	private String email;

	private String phoneNumber;

	@Column(name="picture_url")
	private String profilePictureUrl;

	@Column(nullable = false, updatable = false)
	private LocalDateTime created_at;

	private LocalDateTime updated_at;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Account> accounts = new ArrayList<>();

	@PrePersist
	protected void onCreate() {
		created_at = LocalDateTime.now();
		updated_at = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updated_at = LocalDateTime.now();
	}
}
