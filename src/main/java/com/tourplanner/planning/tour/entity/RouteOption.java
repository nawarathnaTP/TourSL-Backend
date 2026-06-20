package com.tourplanner.planning.tour.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "route_options")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteOption {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "option_id")
	private Long optionId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "route_id", nullable = false)
	private Route route;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transport_id", nullable = false)
	private TransportOption transport;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal distance;

	@Column(nullable = false)
	private Integer time;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal cost;

	@Builder.Default
	@Column(name = "is_selected", nullable = false)
	private boolean isSelected = false;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "selected_at")
	private Instant selectedAt;
}
