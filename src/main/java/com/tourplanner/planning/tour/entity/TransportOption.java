package com.tourplanner.planning.tour.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transport_options")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportOption {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "transport_id")
	private Long transportId;

	@Column(nullable = false, unique = true)
	private String type;
}
