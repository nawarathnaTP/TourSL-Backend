package com.tourplanner.planning.booking.service;

import com.tourplanner.planning.booking.dto.BookingRequest;
import com.tourplanner.planning.booking.dto.BookingResponse;

import java.util.List;
import java.util.UUID;

public interface BookingService {

	BookingResponse createBooking(BookingRequest request);

	BookingResponse confirmBooking(UUID bookingId);

	BookingResponse cancelBooking(UUID bookingId);

	List<BookingResponse> getMyBookings();

	List<BookingResponse> getBookingsForPackage(UUID packageId);
}
