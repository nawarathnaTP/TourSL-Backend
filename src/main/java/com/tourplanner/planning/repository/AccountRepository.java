package com.tourplanner.planning.repository;

import com.tourplanner.planning.entity.Account;
import com.tourplanner.planning.entity.AuthProvider;
import com.tourplanner.planning.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

	Optional<Account> findByUserAndAuthProvider(User user, AuthProvider authProvider);

	Optional<Account> findByGoogleId(String googleId);

	boolean existsByUserAndAuthProvider(User user, AuthProvider authProvider);
}
