package com.tourplanner.planning.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

	private UUID bookingId;
	private UUID packageId;
	private UUID tourId;
	private String tourTitle;
	private UUID touristId;
	private Integer slotsBooked;
	private BigDecimal totalPrice;
	private String status;
	private OffsetDateTime paymentDeadline;
	private OffsetDateTime bookedAt;
	private OffsetDateTime updatedAt;
}
