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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

	private final BookingRepository bookingRepository;
	private final GuideTourPackageRepository guideTourPackageRepository;
	private final UserRepository userRepository;

	/**
	 * Creates a booking and reserves slots immediately (PENDING_PAYMENT).
	 *
	 * Slots are decremented at booking time to prevent race conditions.
	 * A 15-minute payment deadline is set — if not paid in time,
	 * the scheduled job expires the booking and restores slots.
	 *
	 * Optimistic locking via @Version on GuideTourPackage prevents
	 * two concurrent bookings from overselling slots.
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

		// Reserve slots immediately — optimistic locking prevents overselling
		try {
			pkg.setAvailableSlots(pkg.getAvailableSlots() - request.getSlotsBooked());

			if (pkg.getAvailableSlots() == 0) {
				pkg.setStatus(PackageStatus.FILLED);
			}

			guideTourPackageRepository.save(pkg);
		} catch (ObjectOptimisticLockingFailureException e) {
			throw new IllegalArgumentException(
					"Booking failed due to concurrent modification. Please try again.");
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
	 * Slots are already reserved — this just flips status to CONFIRMED.
	 * Rejects if payment deadline has passed (booking will be expired by scheduler).
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

		if (booking.getPaymentDeadline().isBefore(OffsetDateTime.now())) {
			throw new IllegalArgumentException("Payment deadline has expired");
		}

		booking.setStatus(BookingStatus.CONFIRMED);
		return mapToResponse(bookingRepository.save(booking));
	}

	/**
	 * Tourist or guide cancels a booking.
	 *
	 * Slots are always restored since they are reserved at booking time.
	 * If package was FILLED, it goes back to PUBLISHED.
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

		restoreSlots(booking);

		booking.setStatus(BookingStatus.CANCELLED);
		return mapToResponse(bookingRepository.save(booking));
	}

	/**
	 * Expires unpaid bookings past their payment deadline.
	 * Called by the scheduled job. Restores reserved slots.
	 */
	@Transactional
	public void expireUnpaidBookings() {
		List<Booking> expired = bookingRepository.findByStatusAndPaymentDeadlineBefore(
				BookingStatus.PENDING_PAYMENT, OffsetDateTime.now());

		for (Booking booking : expired) {
			restoreSlots(booking);
			booking.setStatus(BookingStatus.CANCELLED);
			bookingRepository.save(booking);
		}
	}

	private void restoreSlots(Booking booking) {
		GuideTourPackage pkg = booking.getGuideTourPackage();
		pkg.setAvailableSlots(pkg.getAvailableSlots() + booking.getSlotsBooked());

		if (pkg.getStatus() == PackageStatus.FILLED) {
			pkg.setStatus(PackageStatus.PUBLISHED);
		}

		guideTourPackageRepository.save(pkg);
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
				.paymentDeadline(booking.getPaymentDeadline())
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
