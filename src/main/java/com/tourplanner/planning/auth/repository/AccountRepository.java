package com.tourplanner.planning.auth.repository;

import com.tourplanner.planning.auth.entity.Account;
import com.tourplanner.planning.auth.entity.AuthProvider;
import com.tourplanner.planning.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

	Optional<Account> findByUserAndAuthProvider(User user, AuthProvider AuthProvider);

	Optional<Account> findByProviderId(String providerId);

	boolean existsByUserAndAuthProvider(User user, AuthProvider AuthProvider);
}
