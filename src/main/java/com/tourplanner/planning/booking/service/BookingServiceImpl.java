package com.tourplanner.planning.booking.service;

import com.tourplanner.planning.auth.entity.Role;
import com.tourplanner.planning.auth.entity.User;
import com.tourplanner.planning.auth.repository.UserRepository;
import com.tourplanner.planning.booking.dto.BookingRequest;
import com.tourplanner.planning.booking.dto.BookingResponse;
import com.tourplanner.planning.booking.entity.Booking;
import com.tourplanner.planning.booking.entity.BookingStatus;
import com.tourplanner.planning.booking.repository.BookingRepository;
import com.tourplanner.planning.tour.entity.GuideTourPackage;
import com.tourplanner.planning.tour.entity.PackageStatus;
import com.tourplanner.planning.tour.repository.GuideTourPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

	private final BookingRepository bookingRepository;
	private final GuideTourPackageRepository guideTourPackageRepository;
	private final UserRepository userRepository;

	/**
	 * Creates a booking for a published guide tour package.
	 *
	 * Overbooking prevention:
	 * - Checks availableSlots >= requested slots before booking
	 * - Uses @Version (optimistic locking) on GuideTourPackage so that if two
	 *   tourists book simultaneously, the second one gets an ObjectOptimisticLockingFailureException
	 *   rather than both succeeding and overselling slots
	 * - availableSlots is decremented atomically within the same transaction
	 * - Auto-sets package status to FILLED when availableSlots hits 0
	 *
	 * ACID compliance:
	 * - @Transactional ensures the slot decrement + booking creation happen atomically
	 * - If either fails, both are rolled back
	 *
	 * Additional validations:
	 * - Only tourists can book (not guides booking their own packages)
	 * - Package must be PUBLISHED
	 * - Duplicate booking prevention (one booking per tourist per package)
	 * - Guides cannot book their own packages
	 */
	@Override
	@Transactional
	public BookingResponse createBooking(BookingRequest request) {
		User tourist = getAuthenticatedUser();

		// Only tourists can book
		if (tourist.getRole() != Role.TOURIST) {
			throw new IllegalArgumentException("Only tourists can book packages");
		}

		GuideTourPackage pkg = guideTourPackageRepository.findById(request.getPackageId())
				.orElseThrow(() -> new RuntimeException("Package not found: " + request.getPackageId()));

		// Package must be published
		if (pkg.getStatus() != PackageStatus.PUBLISHED) {
			throw new IllegalArgumentException("Package is not available for booking");
		}

		// Prevent duplicate bookings
		if (bookingRepository.existsByTourist_IdAndGuideTourPackage_PackageId(tourist.getId(), pkg.getPackageId())) {
			throw new IllegalArgumentException("You have already booked this package");
		}

		// Check slot availability (first check before optimistic lock)
		if (pkg.getAvailableSlots() < request.getSlotsBooked()) {
			throw new IllegalArgumentException(
					"Not enough slots available. Requested: " + request.getSlotsBooked()
					+ ", Available: " + pkg.getAvailableSlots());
		}

		// Decrement available slots — optimistic locking via @Version
		// protects against concurrent modifications
		try {
			pkg.setAvailableSlots(pkg.getAvailableSlots() - request.getSlotsBooked());

			// Auto-fill when no slots remain
			if (pkg.getAvailableSlots() == 0) {
				pkg.setStatus(PackageStatus.FILLED);
			}

			guideTourPackageRepository.save(pkg);
		} catch (ObjectOptimisticLockingFailureException e) {
			throw new IllegalArgumentException(
					"Booking failed due to concurrent modification. Please try again.");
		}

		// Calculate total price
		BigDecimal totalPrice = pkg.getPricePerSlot()
				.multiply(BigDecimal.valueOf(request.getSlotsBooked()));

		// Create booking — within the same transaction as slot decrement
		Booking booking = Booking.builder()
				.guideTourPackage(pkg)
				.tourist(tourist)
				.slotsBooked(request.getSlotsBooked())
				.totalPrice(totalPrice)
				.build();

		return mapToResponse(bookingRepository.save(booking));
	}

	/**
	 * Guide confirms a pending booking.
	 *
	 * Only the guide who owns the package can confirm.
	 */
	@Override
	@Transactional
	public BookingResponse confirmBooking(UUID bookingId) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

		User user = getAuthenticatedUser();
		UUID packageOwnerId = booking.getGuideTourPackage().getTour().getUser().getId();

		if (!packageOwnerId.equals(user.getId())) {
			throw new IllegalArgumentException("You do not own this package");
		}

		if (booking.getStatus() != BookingStatus.PENDING) {
			throw new IllegalArgumentException("Can only confirm PENDING bookings");
		}

		booking.setStatus(BookingStatus.CONFIRMED);
		return mapToResponse(bookingRepository.save(booking));
	}

	/**
	 * Tourist or guide cancels a booking.
	 *
	 * - Tourist can cancel their own booking
	 * - Guide can cancel bookings on their packages
	 * - Cancelled slots are restored back to the package (within same transaction)
	 * - If package was FILLED, it goes back to PUBLISHED
	 */
	@Override
	@Transactional
	public BookingResponse cancelBooking(UUID bookingId) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

		User user = getAuthenticatedUser();
		UUID touristId = booking.getTourist().getId();
		UUID packageOwnerId = booking.getGuideTourPackage().getTour().getUser().getId();

		// Either the tourist or the package owner can cancel
		if (!touristId.equals(user.getId()) && !packageOwnerId.equals(user.getId())) {
			throw new IllegalArgumentException("You are not authorized to cancel this booking");
		}

		if (booking.getStatus() == BookingStatus.CANCELLED) {
			throw new IllegalArgumentException("Booking is already cancelled");
		}

		// Restore slots back to the package
		GuideTourPackage pkg = booking.getGuideTourPackage();
		pkg.setAvailableSlots(pkg.getAvailableSlots() + booking.getSlotsBooked());

		// If package was filled, reopen it
		if (pkg.getStatus() == PackageStatus.FILLED) {
			pkg.setStatus(PackageStatus.PUBLISHED);
		}

		guideTourPackageRepository.save(pkg);

		booking.setStatus(BookingStatus.CANCELLED);
		return mapToResponse(bookingRepository.save(booking));
	}

	/**
	 * Tourist views their own bookings.
	 */
	@Override
	@Transactional(readOnly = true)
	public List<BookingResponse> getMyBookings() {
		User tourist = getAuthenticatedUser();
		return bookingRepository.findByTourist_Id(tourist.getId()).stream()
				.map(this::mapToResponse)
				.toList();
	}

	/**
	 * Guide views all bookings for one of their packages.
	 *
	 * Ownership verified — guide can only see bookings on their own packages.
	 */
	@Override
	@Transactional(readOnly = true)
	public List<BookingResponse> getBookingsForPackage(UUID packageId) {
		User user = getAuthenticatedUser();
		GuideTourPackage pkg = guideTourPackageRepository.findById(packageId)
				.orElseThrow(() -> new RuntimeException("Package not found: " + packageId));

		if (!pkg.getTour().getUser().getId().equals(user.getId())) {
			throw new IllegalArgumentException("You do not own this package");
		}

		return bookingRepository.findByGuideTourPackage_PackageId(packageId).stream()
				.map(this::mapToResponse)
				.toList();
	}

	private BookingResponse mapToResponse(Booking booking) {
		return BookingResponse.builder()
				.bookingId(booking.getBookingId())
				.packageId(booking.getGuideTourPackage().getPackageId())
				.tourTitle(booking.getGuideTourPackage().getTour().getTitle())
				.touristId(booking.getTourist().getId())
				.slotsBooked(booking.getSlotsBooked())
				.totalPrice(booking.getTotalPrice())
				.status(booking.getStatus().name())
				.bookedAt(booking.getBookedAt())
				.updatedAt(booking.getUpdatedAt())
				.build();
	}

	private User getAuthenticatedUser() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("Authenticated user not found"));
	}
}
