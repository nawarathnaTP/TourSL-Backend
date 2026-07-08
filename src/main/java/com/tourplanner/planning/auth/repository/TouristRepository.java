package com.tourplanner.planning.auth.repository;

import com.tourplanner.planning.auth.entity.Tourist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TouristRepository extends JpaRepository<Tourist, UUID> {

	Optional<Tourist> findByUser_Id(UUID userId);
}
