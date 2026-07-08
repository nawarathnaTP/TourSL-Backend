package com.tourplanner.planning.config;

import com.tourplanner.planning.auth.entity.User;
import com.tourplanner.planning.auth.repository.UserRepository;
import com.tourplanner.planning.tour.entity.GuideTourPackage;
import com.tourplanner.planning.tour.entity.PackageStatus;
import com.tourplanner.planning.tour.entity.Tour;
import com.tourplanner.planning.tour.entity.TourType;
import com.tourplanner.planning.tour.repository.GuideTourPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TourAccessValidator {

    private final UserRepository userRepository;
    private final GuideTourPackageRepository guideTourPackageRepository;

    /**
     * Verifies the authenticated user owns this tour.
     * Throws if someone else tries to modify it.
     */
    public void verifyOwnership(Tour tour) {
        User user = getAuthenticatedUser();
        if (!tour.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You do not own this tour");
        }
    }

    /**
     * Verifies the tour is still modifiable.
     * Tourist tours are always modifiable.
     * Guide tours are only modifiable when the package is in DRAFT status.
     */
    public void verifyModifiable(Tour tour) {
        if (tour.getTourType() != TourType.GUIDE) {
            return;
        }

        GuideTourPackage pkg = guideTourPackageRepository.findByTour_TourId(tour.getTourId())
                .orElse(null);

        if (pkg != null && pkg.getStatus() != PackageStatus.DRAFT) {
            throw new IllegalArgumentException(
                    "Cannot modify a guide tour that is " + pkg.getStatus().name().toLowerCase());
        }
    }

    /**
     * Convenience method: verifies both ownership and modifiability.
     */
    public void verifyOwnershipAndModifiable(Tour tour) {
        verifyOwnership(tour);
        verifyModifiable(tour);
    }

    public User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
