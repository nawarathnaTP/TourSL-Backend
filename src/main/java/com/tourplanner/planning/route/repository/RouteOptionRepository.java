package com.tourplanner.planning.route.repository;

import com.tourplanner.planning.route.entity.RouteOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RouteOptionRepository extends JpaRepository<RouteOption, UUID> {
}
