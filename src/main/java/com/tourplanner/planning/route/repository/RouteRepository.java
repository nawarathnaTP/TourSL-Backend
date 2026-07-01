package com.tourplanner.planning.route.repository;

import com.tourplanner.planning.route.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {

    Optional<Route> findByStartStop_StopIdAndEndStop_StopId(UUID startStopId, UUID endStopId);

    @Query("SELECT r FROM Route r WHERE r.startStop.day.dayId = :dayId ORDER BY r.startStop.stopOrder")
    List<Route> findByDayId(UUID dayId);

    @Query("SELECT r FROM Route r WHERE r.startStop.stopId = :stopId OR r.endStop.stopId = :stopId")
    List<Route> findByStopId(UUID stopId);
}
