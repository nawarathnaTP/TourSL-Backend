package com.tourplanner.planning.booking.scheduler;

import com.tourplanner.planning.booking.service.BookingServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {

	private final BookingServiceImpl bookingService;

	/**
	 * Runs every minute to cancel unpaid bookings past their payment deadline
	 * and restore reserved slots.
	 */
	@Scheduled(fixedRate = 60000)
	public void expireUnpaidBookings() {
		bookingService.expireUnpaidBookings();
	}
}
