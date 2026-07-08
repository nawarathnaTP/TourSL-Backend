package com.tourplanner.planning.booking.controller;

import com.tourplanner.planning.booking.dto.BookingRequest;
import com.tourplanner.planning.booking.dto.BookingResponse;
import com.tourplanner.planning.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

	private final BookingService bookingService;

	@PostMapping
	public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request));
	}

	@PatchMapping("/{bookingId}/confirm")
	public ResponseEntity<BookingResponse> confirmBooking(@PathVariable UUID bookingId) {
		return ResponseEntity.ok(bookingService.confirmBooking(bookingId));
	}

	@PatchMapping("/{bookingId}/cancel")
	public ResponseEntity<BookingResponse> cancelBooking(@PathVariable UUID bookingId) {
		return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
	}

	@GetMapping("/my-bookings")
	public ResponseEntity<List<BookingResponse>> getMyBookings() {
		return ResponseEntity.ok(bookingService.getMyBookings());
	}

	@GetMapping("/package/{packageId}")
	public ResponseEntity<List<BookingResponse>> getBookingsForPackage(@PathVariable UUID packageId) {
		return ResponseEntity.ok(bookingService.getBookingsForPackage(packageId));
	}
}
