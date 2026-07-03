package com.example.moneyquest.app.domain.service;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.moneyquest.app.infra.entity.UserEntity;

/**
 * Spring Security の認証情報。
 * ログイン中ユーザーの UserEntity を保持し、
 * authority(Integer) を ROLE_XXX 形式の権限に変換して提供する。
 */
public class CustomUserDetails implements UserDetails {

	/** authority の値の意味（DB の user_t.authority と対応） */
	public static final int AUTHORITY_ADMIN = 0;
	public static final int AUTHORITY_PARENT = 1;
	public static final int AUTHORITY_CHILD = 2;

	private final UserEntity user;

	public CustomUserDetails(UserEntity user) {
		this.user = user;
	}

	/** ログイン中ユーザーの userId（UserService から利用） */
	public Integer getUserId() {
		return user.getUserId();
	}

	/** ログイン中ユーザーのエンティティを取り出す（業務で使う場合） */
	public UserEntity getUser() {
		return user;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		String role = switch (user.getAuthority()) {
		case AUTHORITY_ADMIN -> "ROLE_ADMIN";
		case AUTHORITY_PARENT -> "ROLE_PARENT";
		case AUTHORITY_CHILD -> "ROLE_CHILD";
		default -> throw new IllegalStateException("不正な権限値: " + user.getAuthority());
		};
		return List.of(new SimpleGrantedAuthority(role));
	}

	@Override
	public String getPassword() {
		return user.getPassword();
	}

	@Override
	public String getUsername() {
		return user.getMailAddress();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}