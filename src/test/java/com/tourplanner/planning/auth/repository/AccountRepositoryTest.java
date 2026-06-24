package com.tourplanner.planning.auth.repository;

import com.tourplanner.planning.auth.entity.Account;
import com.tourplanner.planning.auth.entity.AuthProvider;
import com.tourplanner.planning.auth.entity.User;
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
				.provider(AuthProvider.LOCAL)
				.password("encoded-password")
				.build());

		Optional<Account> found = accountRepository.findByUser__AndProvider(savedUser, AuthProvider.LOCAL);

		assertThat(found).isPresent();
		assertThat(found.get().getProvider()).isEqualTo(AuthProvider.LOCAL);
		assertThat(found.get().getPassword()).isEqualTo("encoded-password");
	}

	@Test
	void findByUserAndAuthProvider_noMatchingProvider_returnsEmpty() {
		accountRepository.save(Account.builder()
				.user(savedUser)
				.provider(AuthProvider.LOCAL)
				.password("encoded-password")
				.build());

		Optional<Account> found = accountRepository.findByUser__AndProvider(savedUser, AuthProvider.GOOGLE);

		assertThat(found).isEmpty();
	}

	@Test
	void findByGoogleId_existingGoogleId_returnsAccount() {
		accountRepository.save(Account.builder()
				.user(savedUser)
				.provider(AuthProvider.GOOGLE)
				.provider_user_id("google-123")
				.build());

		Optional<Account> found = accountRepository.findByProvider_user_id("google-123");

		assertThat(found).isPresent();
		assertThat(found.get().getProvider_user_id()).isEqualTo("google-123");
	}

	@Test
	void findByGoogleId_nonExistingGoogleId_returnsEmpty() {
		Optional<Account> found = accountRepository.findByProvider_user_id("non-existent");

		assertThat(found).isEmpty();
	}

	@Test
	void existsByUser__AndProvider_exists_returnsTrue() {
		accountRepository.save(Account.builder()
				.user(savedUser)
				.provider(AuthProvider.LOCAL)
				.password("encoded-password")
				.build());

		assertThat(accountRepository.existsByUser__AndProvider(savedUser, AuthProvider.LOCAL)).isTrue();
	}

	@Test
	void existsByUser__AndProvider_notExists_returnsFalse() {
		assertThat(accountRepository.existsByUser__AndProvider(savedUser, AuthProvider.GOOGLE)).isFalse();
	}

	@Test
	void save_setsCreatedAtAndUpdatedAt() {
		Account saved = accountRepository.save(Account.builder()
				.user(savedUser)
				.provider(AuthProvider.LOCAL)
				.password("encoded-password")
				.build());

		assertThat(saved.getAcc_id()).isNotNull();
		assertThat(saved.getCreated_at()).isNotNull();
		assertThat(saved.getUpdated_at()).isNotNull();
	}
}
