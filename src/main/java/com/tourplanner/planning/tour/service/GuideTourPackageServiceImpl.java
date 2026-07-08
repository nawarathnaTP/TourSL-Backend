package com.tourplanner.planning.tour.service;

import com.tourplanner.planning.auth.entity.User;
import com.tourplanner.planning.auth.repository.UserRepository;
import com.tourplanner.planning.booking.entity.BookingStatus;
import com.tourplanner.planning.booking.repository.BookingRepository;
import com.tourplanner.planning.tour.dto.GuideTourPackageRequest;
import com.tourplanner.planning.tour.dto.GuideTourPackageResponse;
import com.tourplanner.planning.tour.entity.GuideTourPackage;
import com.tourplanner.planning.tour.entity.PackageStatus;
import com.tourplanner.planning.tour.repository.GuideTourPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuideTourPackageServiceImpl implements GuideTourPackageService {

    private final GuideTourPackageRepository guideTourPackageRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public GuideTourPackageResponse getPackageByTourId(UUID tourId) {
        GuideTourPackage pkg = guideTourPackageRepository.findByTour_TourId(tourId)
                .orElseThrow(() -> new RuntimeException("Package not found for tour: " + tourId));
        return mapToResponse(pkg);
    }

    @Override
    @Transactional
    public GuideTourPackageResponse updatePackage(UUID tourId, GuideTourPackageRequest request) {
        GuideTourPackage pkg = getOwnedPackage(tourId);

        if (pkg.getStatus() != PackageStatus.DRAFT) {
            throw new IllegalArgumentException("Can only update packages in DRAFT status");
        }

        pkg.setDescription(request.getDescription());
        pkg.setCoverImageUrl(request.getCoverImageUrl());
        pkg.setMaxSlots(request.getMaxSlots());
        pkg.setAvailableSlots(request.getMaxSlots());
        pkg.setPricePerSlot(request.getPricePerSlot());

        return mapToResponse(guideTourPackageRepository.save(pkg));
    }

    @Override
    @Transactional
    public GuideTourPackageResponse publishPackage(UUID tourId) {
        GuideTourPackage pkg = getOwnedPackage(tourId);

        if (pkg.getMaxSlots() == null || pkg.getPricePerSlot() == null) {
            throw new IllegalArgumentException("Package details (maxSlots, pricePerSlot) must be set before publishing");
        }

        if (pkg.getStatus() != PackageStatus.DRAFT) {
            throw new IllegalArgumentException("Can only publish packages in DRAFT status");
        }

        pkg.setIsPublished(true);
        pkg.setStatus(PackageStatus.PUBLISHED);

        return mapToResponse(guideTourPackageRepository.save(pkg));
    }

    @Override
    @Transactional
    public GuideTourPackageResponse cancelPackage(UUID tourId) {
        GuideTourPackage pkg = getOwnedPackage(tourId);

        pkg.setIsPublished(false);
        pkg.setStatus(PackageStatus.CANCELLED);

        return mapToResponse(guideTourPackageRepository.save(pkg));
    }

    @Override
    @Transactional
    public GuideTourPackageResponse unpublishPackage(UUID tourId) {
        GuideTourPackage pkg = getOwnedPackage(tourId);

        if (pkg.getStatus() != PackageStatus.PUBLISHED) {
            throw new IllegalArgumentException("Can only unpublish packages that are currently PUBLISHED");
        }

        boolean hasActiveBookings = bookingRepository.existsByGuideTourPackage_PackageIdAndStatusIn(
                pkg.getPackageId(), List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED));

        if (hasActiveBookings) {
            throw new IllegalArgumentException("Cannot revert to draft while there are active bookings");
        }

        pkg.setIsPublished(false);
        pkg.setStatus(PackageStatus.DRAFT);

        return mapToResponse(guideTourPackageRepository.save(pkg));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuideTourPackageResponse> getMyPackages() {
        User user = getAuthenticatedUser();
        return guideTourPackageRepository.findByTour_User_Id(user.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuideTourPackageResponse> getPublishedPackages() {
        return guideTourPackageRepository.findByIsPublishedTrue().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private GuideTourPackage getOwnedPackage(UUID tourId) {
        User user = getAuthenticatedUser();
        GuideTourPackage pkg = guideTourPackageRepository.findByTour_TourId(tourId)
                .orElseThrow(() -> new RuntimeException("Package not found for tour: " + tourId));

        if (!pkg.getTour().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You do not own this package");
        }

        return pkg;
    }

    private GuideTourPackageResponse mapToResponse(GuideTourPackage pkg) {
        return GuideTourPackageResponse.builder()
                .packageId(pkg.getPackageId())
                .tourId(pkg.getTour().getTourId())
                .tourTitle(pkg.getTour().getTitle())
                .startDay(pkg.getTour().getStartDay())
                .endDay(pkg.getTour().getEndDay())
                .description(pkg.getDescription())
                .coverImageUrl(pkg.getCoverImageUrl())
                .maxSlots(pkg.getMaxSlots())
                .availableSlots(pkg.getAvailableSlots())
                .pricePerSlot(pkg.getPricePerSlot())
                .isPublished(pkg.getIsPublished())
                .status(pkg.getStatus().name())
                .build();
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
