package com.tourplanner.planning.tour.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "tour_stops", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"tour_id", "location_no"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourStop {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "stop_id")
	private Long stopId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tour_id", nullable = false)
	private Tour tour;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_id", nullable = false)
	private Location location;

	@Column(name = "location_no", nullable = false)
	private Integer locationNo;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
