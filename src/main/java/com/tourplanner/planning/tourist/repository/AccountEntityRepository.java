package com.tourplanner.planning.tourist.repository;

import com.tourplanner.planning.tourist.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccountEntityRepository extends JpaRepository<AccountEntity, UUID> {
}
