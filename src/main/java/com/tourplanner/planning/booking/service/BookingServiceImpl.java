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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

	private final BookingRepository bookingRepository;
	private final GuideTourPackageRepository guideTourPackageRepository;
	private final UserRepository userRepository;

	/**
	 * Creates a booking in PENDING_PAYMENT state.
	 *
	 * Slots are NOT decremented yet — that happens on payment.
	 * Validates: tourist role, package published, tour not started,
	 * no duplicate bookings, and sufficient slots available.
	 */
	@Override
	@Transactional
	public BookingResponse createBooking(BookingRequest request) {
		User tourist = getAuthenticatedUser();

		if (tourist.getRole() != Role.TOURIST) {
			throw new IllegalArgumentException("Only tourists can book packages");
		}

		GuideTourPackage pkg = guideTourPackageRepository.findById(request.getPackageId())
				.orElseThrow(() -> new RuntimeException("Package not found: " + request.getPackageId()));

		if (pkg.getStatus() != PackageStatus.PUBLISHED) {
			throw new IllegalArgumentException("Package is not available for booking");
		}

		if (!pkg.getTour().getStartDay().isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("Cannot book a tour that has already started or passed");
		}

		if (bookingRepository.existsByTourist_IdAndGuideTourPackage_PackageId(tourist.getId(), pkg.getPackageId())) {
			throw new IllegalArgumentException("You have already booked this package");
		}

		if (pkg.getAvailableSlots() < request.getSlotsBooked()) {
			throw new IllegalArgumentException(
					"Not enough slots available. Requested: " + request.getSlotsBooked()
					+ ", Available: " + pkg.getAvailableSlots());
		}

		BigDecimal totalPrice = pkg.getPricePerSlot()
				.multiply(BigDecimal.valueOf(request.getSlotsBooked()));

		Booking booking = Booking.builder()
				.guideTourPackage(pkg)
				.tourist(tourist)
				.slotsBooked(request.getSlotsBooked())
				.totalPrice(totalPrice)
				.build();

		return mapToResponse(bookingRepository.save(booking));
	}

	/**
	 * Simulates payment and auto-confirms the booking.
	 *
	 * On successful payment:
	 * - Decrements available slots (with optimistic locking for overbooking prevention)
	 * - Sets booking status to CONFIRMED
	 * - Auto-sets package to FILLED when no slots remain
	 *
	 * When Stripe is integrated, this logic moves to the webhook handler.
	 */
	@Override
	@Transactional
	public BookingResponse payBooking(UUID bookingId) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

		User user = getAuthenticatedUser();
		if (!booking.getTourist().getId().equals(user.getId())) {
			throw new IllegalArgumentException("You can only pay for your own bookings");
		}

		if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
			throw new IllegalArgumentException("Booking is not awaiting payment");
		}

		GuideTourPackage pkg = booking.getGuideTourPackage();

		if (pkg.getAvailableSlots() < booking.getSlotsBooked()) {
			throw new IllegalArgumentException(
					"Not enough slots available. Requested: " + booking.getSlotsBooked()
					+ ", Available: " + pkg.getAvailableSlots());
		}

		try {
			pkg.setAvailableSlots(pkg.getAvailableSlots() - booking.getSlotsBooked());

			if (pkg.getAvailableSlots() == 0) {
				pkg.setStatus(PackageStatus.FILLED);
			}

			guideTourPackageRepository.save(pkg);
		} catch (ObjectOptimisticLockingFailureException e) {
			throw new IllegalArgumentException(
					"Payment failed due to concurrent modification. Please try again.");
		}

		booking.setStatus(BookingStatus.CONFIRMED);
		return mapToResponse(bookingRepository.save(booking));
	}

	/**
	 * Tourist or guide cancels a booking.
	 *
	 * - Tourist can cancel their own booking
	 * - Guide can cancel bookings on their packages
	 * - Slots are only restored if the booking was CONFIRMED (payment was made)
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

		if (!touristId.equals(user.getId()) && !packageOwnerId.equals(user.getId())) {
			throw new IllegalArgumentException("You are not authorized to cancel this booking");
		}

		if (booking.getStatus() == BookingStatus.CANCELLED) {
			throw new IllegalArgumentException("Booking is already cancelled");
		}

		// Only restore slots if payment was made (CONFIRMED)
		if (booking.getStatus() == BookingStatus.CONFIRMED) {
			GuideTourPackage pkg = booking.getGuideTourPackage();
			pkg.setAvailableSlots(pkg.getAvailableSlots() + booking.getSlotsBooked());

			if (pkg.getStatus() == PackageStatus.FILLED) {
				pkg.setStatus(PackageStatus.PUBLISHED);
			}

			guideTourPackageRepository.save(pkg);
		}

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
				.tourId(booking.getGuideTourPackage().getTour().getTourId())
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
