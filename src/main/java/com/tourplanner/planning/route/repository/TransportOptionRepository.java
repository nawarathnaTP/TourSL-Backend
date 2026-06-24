package com.tourplanner.planning.route.repository;

import com.tourplanner.planning.route.entity.TransportOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransportOptionRepository extends JpaRepository<TransportOption, UUID> {
}
