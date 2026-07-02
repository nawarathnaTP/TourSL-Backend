package com.tourplanner.planning.tour.repository;

import com.tourplanner.planning.tour.entity.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TourRepository extends JpaRepository<Tour, UUID> {
    List<Tour> findByUser_Id(UUID userId);

    @Query("SELECT COUNT(t) > 0 FROM Tour t WHERE t.user.id = :userId AND t.startDay <= :endDay AND t.endDay >= :startDay")
    boolean existsOverlappingTour(UUID userId, LocalDate startDay, LocalDate endDay);

    @Query("SELECT COUNT(t) > 0 FROM Tour t WHERE t.user.id = :userId AND t.tourId <> :tourId AND t.startDay <= :endDay AND t.endDay >= :startDay")
    boolean existsOverlappingTourExcluding(UUID userId, UUID tourId, LocalDate startDay, LocalDate endDay);
}
