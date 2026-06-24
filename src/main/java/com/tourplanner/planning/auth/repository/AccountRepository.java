package com.tourplanner.planning.auth.repository;

import com.tourplanner.planning.auth.entity.Account;
import com.tourplanner.planning.auth.entity.AuthProvider;
import com.tourplanner.planning.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

	@Query("SELECT a FROM Account a WHERE a.user = :user AND a.provider = :provider")
	Optional<Account> findByUser__AndProvider(@Param("user") User user, @Param("provider") AuthProvider provider);

	@Query("SELECT a FROM Account a WHERE a.provider_user_id = :provider_user_id")
	Optional<Account> findByProvider_user_id(@Param("provider_user_id") String provider_user_id);

	@Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Account a WHERE a.user = :user AND a.provider = :provider")
	boolean existsByUser__AndProvider(@Param("user") User user, @Param("provider") AuthProvider provider);
}
