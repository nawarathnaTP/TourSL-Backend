package com.tourplanner.planning.booking.repository;

import com.tourplanner.planning.booking.entity.Booking;
import com.tourplanner.planning.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

	List<Booking> findByTourist_Id(UUID touristId);

	List<Booking> findByGuideTourPackage_PackageId(UUID packageId);

	List<Booking> findByTourist_IdAndStatus(UUID touristId, BookingStatus status);

	boolean existsByTourist_IdAndGuideTourPackage_PackageId(UUID touristId, UUID packageId);

	boolean existsByGuideTourPackage_PackageIdAndStatusIn(UUID packageId, List<BookingStatus> statuses);
}
