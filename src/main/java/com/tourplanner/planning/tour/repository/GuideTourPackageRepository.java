package com.tourplanner.planning.tour.repository;

import com.tourplanner.planning.tour.entity.GuideTourPackage;
import com.tourplanner.planning.tour.entity.PackageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuideTourPackageRepository extends JpaRepository<GuideTourPackage, UUID> {

    Optional<GuideTourPackage> findByTour_TourId(UUID tourId);

    List<GuideTourPackage> findByIsPublishedTrue();

    List<GuideTourPackage> findByStatus(PackageStatus status);

    List<GuideTourPackage> findByTour_User_Id(UUID userId);
}
