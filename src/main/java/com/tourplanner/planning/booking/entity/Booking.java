package com.tourplanner.planning.booking.entity;

import com.tourplanner.planning.auth.entity.User;
import com.tourplanner.planning.tour.entity.GuideTourPackage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "booking_id", updatable = false, nullable = false)
	private UUID bookingId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "package_id", nullable = false)
	private GuideTourPackage guideTourPackage;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tourist_id", nullable = false)
	private User tourist;

	@Column(name = "slots_booked", nullable = false)
	private Integer slotsBooked;

	@Column(name = "total_price", nullable = false)
	private BigDecimal totalPrice;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	@Column(nullable = false)
	private BookingStatus status = BookingStatus.PENDING_PAYMENT;

	@Column(name = "payment_deadline", nullable = false, updatable = false)
	private OffsetDateTime paymentDeadline;

	@Column(name = "booked_at", nullable = false, updatable = false)
	private OffsetDateTime bookedAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	private static final int PAYMENT_DEADLINE_MINUTES = 15;

	@PrePersist
	protected void onCreate() {
		bookedAt = OffsetDateTime.now();
		updatedAt = OffsetDateTime.now();
		paymentDeadline = bookedAt.plusMinutes(PAYMENT_DEADLINE_MINUTES);
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = OffsetDateTime.now();
	}
}
