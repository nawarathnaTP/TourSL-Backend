package com.tourplanner.planning.tourist.repository;

import com.tourplanner.planning.tourist.entity.Tourist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TouristRepository extends JpaRepository<Tourist, UUID> {
    Optional<Tourist> findByEmail(String email);
}
