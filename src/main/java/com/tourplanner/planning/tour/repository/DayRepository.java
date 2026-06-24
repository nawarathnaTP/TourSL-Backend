package com.tourplanner.planning.tour.repository;

import com.tourplanner.planning.tour.entity.Day;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DayRepository extends JpaRepository<Day, UUID> {
    List<Day> findByTour_TourIdOrderByDayNo(UUID tourId);
}
