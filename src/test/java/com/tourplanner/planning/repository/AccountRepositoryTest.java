package com.tourplanner.planning.repository;

import com.tourplanner.planning.entity.Account;
import com.tourplanner.planning.entity.AuthProvider;
import com.tourplanner.planning.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AccountRepositoryTest {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private UserRepository userRepository;

	private User savedUser;

	@BeforeEach
	void setUp() {
		accountRepository.deleteAll();
		userRepository.deleteAll();

		savedUser = userRepository.save(User.builder()
				.firstName("Jane")
				.lastName("Doe")
				.email("jane@example.com")
				.build());
	}

	@Test
	void findByUserAndAuthProvider_existingLocalAccount_returnsAccount() {
		accountRepository.save(Account.builder()
				.user(savedUser)
				.authProvider(AuthProvider.LOCAL)
				.password("encoded-password")
				.build());

		Optional<Account> found = accountRepository.findByUserAndAuthProvider(savedUser, AuthProvider.LOCAL);

		assertThat(found).isPresent();
		assertThat(found.get().getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
		assertThat(found.get().getPassword()).isEqualTo("encoded-password");
	}

	@Test
	void findByUserAndAuthProvider_noMatchingProvider_returnsEmpty() {
		accountRepository.save(Account.builder()
				.user(savedUser)
				.authProvider(AuthProvider.LOCAL)
				.password("encoded-password")
				.build());

		Optional<Account> found = accountRepository.findByUserAndAuthProvider(savedUser, AuthProvider.GOOGLE);

		assertThat(found).isEmpty();
	}

	@Test
	void findByGoogleId_existingGoogleId_returnsAccount() {
		accountRepository.save(Account.builder()
				.user(savedUser)
				.authProvider(AuthProvider.GOOGLE)
				.googleId("google-123")
				.build());

		Optional<Account> found = accountRepository.findByGoogleId("google-123");

		assertThat(found).isPresent();
		assertThat(found.get().getGoogleId()).isEqualTo("google-123");
	}

	@Test
	void findByGoogleId_nonExistingGoogleId_returnsEmpty() {
		Optional<Account> found = accountRepository.findByGoogleId("non-existent");

		assertThat(found).isEmpty();
	}

	@Test
	void existsByUserAndAuthProvider_exists_returnsTrue() {
		accountRepository.save(Account.builder()
				.user(savedUser)
				.authProvider(AuthProvider.LOCAL)
				.password("encoded-password")
				.build());

		assertThat(accountRepository.existsByUserAndAuthProvider(savedUser, AuthProvider.LOCAL)).isTrue();
	}

	@Test
	void existsByUserAndAuthProvider_notExists_returnsFalse() {
		assertThat(accountRepository.existsByUserAndAuthProvider(savedUser, AuthProvider.GOOGLE)).isFalse();
	}

	@Test
	void save_setsCreatedAtAndUpdatedAt() {
		Account saved = accountRepository.save(Account.builder()
				.user(savedUser)
				.authProvider(AuthProvider.LOCAL)
				.password("encoded-password")
				.build());

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
	}
}
