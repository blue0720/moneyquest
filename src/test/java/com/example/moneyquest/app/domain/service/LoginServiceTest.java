package com.example.moneyquest.app.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.infra.repository.UserRepository;

/**
 * LoginService の単体テスト。
 * ログイン認証における「メールアドレス未登録」時の例外ハンドリングを確認する。
 */
@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private LoginService loginService;

	@Test
	@DisplayName("登録済みメールアドレスの場合はCustomUserDetailsを返す")
	void loadUserByUsername_success() {
		UserEntity user = new UserEntity();
		user.setUserId(1);
		user.setMailAddress("parent@example.com");
		user.setPassword("ENCODED");
		user.setAuthority(CustomUserDetails.AUTHORITY_PARENT);

		when(userRepository.findByMailAddress("parent@example.com")).thenReturn(Optional.of(user));

		UserDetails result = loginService.loadUserByUsername("parent@example.com");

		assertThat(result).isInstanceOf(CustomUserDetails.class);
		assertThat(result.getUsername()).isEqualTo("parent@example.com");
	}

	@Test
	@DisplayName("未登録メールアドレスの場合はUsernameNotFoundExceptionを投げる")
	void loadUserByUsername_notFound_throws() {
		when(userRepository.findByMailAddress("unknown@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> loginService.loadUserByUsername("unknown@example.com"))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessageContaining("メールアドレスまたはパスワードが誤っています");
	}
}
