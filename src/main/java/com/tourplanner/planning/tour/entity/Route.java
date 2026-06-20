package com.tourplanner.planning.tour.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "route_id")
	private Long routeId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "start_id", nullable = false)
	private TourStop start;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "destination_id", nullable = false)
	private TourStop destination;

	@OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<RouteOption> options = new ArrayList<>();
}
