package com.example.moneyquest.app.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.moneyquest.app.infra.entity.UserEntity;

/**
 * CustomUserDetails の単体テスト。
 * authority値からROLE文字列への変換、および不正な権限値の場合の例外ハンドリングを確認する。
 */
class CustomUserDetailsTest {

	private UserEntity userWithAuthority(Integer authority) {
		UserEntity user = new UserEntity();
		user.setUserId(1);
		user.setMailAddress("test@example.com");
		user.setPassword("ENCODED");
		user.setAuthority(authority);
		return user;
	}

	@Test
	@DisplayName("authority=0(管理者)の場合はROLE_ADMINを返す")
	void getAuthorities_admin() {
		CustomUserDetails details = new CustomUserDetails(userWithAuthority(CustomUserDetails.AUTHORITY_ADMIN));

		assertThat(details.getAuthorities())
				.extracting(Object::toString)
				.containsExactly("ROLE_ADMIN");
	}

	@Test
	@DisplayName("authority=1(保護者)の場合はROLE_PARENTを返す")
	void getAuthorities_parent() {
		CustomUserDetails details = new CustomUserDetails(userWithAuthority(CustomUserDetails.AUTHORITY_PARENT));

		assertThat(details.getAuthorities())
				.extracting(Object::toString)
				.containsExactly("ROLE_PARENT");
	}

	@Test
	@DisplayName("authority=2(子供)の場合はROLE_CHILDを返す")
	void getAuthorities_child() {
		CustomUserDetails details = new CustomUserDetails(userWithAuthority(CustomUserDetails.AUTHORITY_CHILD));

		assertThat(details.getAuthorities())
				.extracting(Object::toString)
				.containsExactly("ROLE_CHILD");
	}

	@Test
	@DisplayName("不正な権限値の場合はIllegalStateExceptionを投げる")
	void getAuthorities_invalidAuthority_throws() {
		CustomUserDetails details = new CustomUserDetails(userWithAuthority(99));

		assertThatThrownBy(details::getAuthorities)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("不正な権限値");
	}

	@Test
	@DisplayName("getUserId/getUser/getUsername/getPasswordは委譲元のUserEntityの値を返す")
	void delegatesToUserEntity() {
		UserEntity user = userWithAuthority(CustomUserDetails.AUTHORITY_PARENT);
		CustomUserDetails details = new CustomUserDetails(user);

		assertThat(details.getUserId()).isEqualTo(1);
		assertThat(details.getUser()).isSameAs(user);
		assertThat(details.getUsername()).isEqualTo("test@example.com");
		assertThat(details.getPassword()).isEqualTo("ENCODED");
		assertThat(details.isAccountNonExpired()).isTrue();
		assertThat(details.isAccountNonLocked()).isTrue();
		assertThat(details.isCredentialsNonExpired()).isTrue();
		assertThat(details.isEnabled()).isTrue();
	}
}
