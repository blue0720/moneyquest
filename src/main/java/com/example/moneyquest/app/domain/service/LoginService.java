package com.example.moneyquest.app.domain.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.infra.repository.UserRepository;

/**
 * Spring Securityによる認証処理
 */
@Service
public class LoginService implements UserDetailsService {

	private final UserRepository userRepository;

	public LoginService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String mailAddress) throws UsernameNotFoundException {
		UserEntity user = userRepository.findByMailAddress(mailAddress)
				.orElseThrow(() -> new UsernameNotFoundException("メールアドレスまたはパスワードが誤っています"));
		return new CustomUserDetails(user);
	}
}