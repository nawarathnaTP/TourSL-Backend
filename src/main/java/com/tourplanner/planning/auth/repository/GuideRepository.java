package com.tourplanner.planning.auth.repository;

import com.tourplanner.planning.auth.entity.Guide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GuideRepository extends JpaRepository<Guide, UUID> {

	Optional<Guide> findByUser_Id(UUID userId);
}
