package com.tourplanner.planning.stop.repository;

import com.tourplanner.planning.stop.entity.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StopRepository extends JpaRepository<Stop, UUID> {
    List<Stop> findByDay_DayIdOrderByStopOrder(UUID dayId);
}
