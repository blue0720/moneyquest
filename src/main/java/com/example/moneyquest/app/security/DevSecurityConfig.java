package com.example.moneyquest.app.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword;

/**
 * 開発用:Spring Security 設定（認証なし）
 *
 *
 * 切り替え
 *  開発中（認証オフ）: spring.profiles.active=dev   ← このクラスが有効
 *  本番  （認証オン）: spring.profiles.active=prod  ← SecurityConfig が有効
 *
 * 全URLを認証なしで通す
 */

@Configuration
@EnableWebSecurity
@Profile("dev")
public class DevSecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/",
								TransitionTargetPageNameKeyword.TITLE,
								TransitionTargetPageNameKeyword.LOGIN_PARENT,
								TransitionTargetPageNameKeyword.LOGIN_CHILD,
								TransitionTargetPageNameKeyword.LOGIN_ADMIN,
								TransitionTargetPageNameKeyword.REGISTER_PARENT,
								TransitionTargetPageNameKeyword.ERROR,
								TransitionTargetPageNameKeyword.ERROR_ADMIN,
								"/login",
								"/css/**", "/js/**", "/images/**", "/webjars/**")
						.permitAll()
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.requestMatchers("/child/**").hasRole("CHILD")
						.requestMatchers("/parent/**").hasRole("PARENT")
						.anyRequest().authenticated())
				.formLogin(form -> form
						.loginProcessingUrl("/login")
						.usernameParameter("mailAddress")
						.passwordParameter("password")
						.successHandler(successHandler())
						.failureHandler(failureHandler())
						.permitAll())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, authException) -> {
							String uri = request.getRequestURI();
							String userRole = (String) request.getSession().getAttribute("userRole");

							// ログイン済みの場合はuserRoleのみで判断（URLは無視）
							if (userRole != null) {
								if ("ROLE_ADMIN".equals(userRole)) {
									response.sendRedirect(request.getContextPath() + "/error?type=admin");
								} else {
									// 一般ユーザーはURLに関係なくuser-session-error
									response.sendRedirect(request.getContextPath() + "/error?type=user");
								}
								return;
							}

							// 未ログインの場合はアクセスURLで判断
							if (uri.startsWith("/admin")) {
								response.sendRedirect(request.getContextPath() + "/error?type=admin");
							} else {
								response.sendRedirect(request.getContextPath() + "/error?type=user");
							}
						}))
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessHandler((request, response, authentication) -> {
							String target = "/";
							if (authentication != null) {
								for (var ga : authentication.getAuthorities()) {
									if ("ROLE_ADMIN".equals(ga.getAuthority())) {
										target = TransitionTargetPageNameKeyword.LOGIN_ADMIN;
										break;
									}
								}
							}
							response.sendRedirect(request.getContextPath() + target);
						})
						.invalidateHttpSession(true)
						.deleteCookies("JSESSIONID")
						.permitAll())
				.csrf(csrf -> csrf.disable());
		return http.build();
	}

	private AuthenticationSuccessHandler successHandler() {
		return (request, response, authentication) -> {
			String referer = request.getHeader("Referer");
			String target = "/";

			for (GrantedAuthority ga : authentication.getAuthorities()) {
				String role = ga.getAuthority();

				if ("ROLE_ADMIN".equals(role)) {
					if (referer != null && referer.contains(TransitionTargetPageNameKeyword.LOGIN_ADMIN)) {
						target = TransitionTargetPageNameKeyword.ADMIN_PARENTS;
					} else {
						// 子供・保護者ログイン画面から管理者がログインしようとした場合
						// → そのログイン画面にエラーで戻す
						String errorTarget = TransitionTargetPageNameKeyword.LOGIN_PARENT + "?error";
						if (referer != null && referer.contains(TransitionTargetPageNameKeyword.LOGIN_CHILD)) {
							errorTarget = TransitionTargetPageNameKeyword.LOGIN_CHILD + "?error";
						}
						response.sendRedirect(request.getContextPath() + errorTarget);
						return;
					}
				} else if ("ROLE_PARENT".equals(role)) {
					if (referer != null && referer.contains(TransitionTargetPageNameKeyword.LOGIN_PARENT)) {
						target = TransitionTargetPageNameKeyword.PARENT_HOME;
					} else {
						String errorTarget = TransitionTargetPageNameKeyword.LOGIN_PARENT + "?error";
						if (referer != null && referer.contains(TransitionTargetPageNameKeyword.LOGIN_CHILD)) {
							errorTarget = TransitionTargetPageNameKeyword.LOGIN_CHILD + "?error";
						} else if (referer != null && referer.contains(TransitionTargetPageNameKeyword.LOGIN_ADMIN)) {
							errorTarget = TransitionTargetPageNameKeyword.LOGIN_ADMIN + "?error";
						}
						response.sendRedirect(request.getContextPath() + errorTarget);
						return;
					}
				} else if ("ROLE_CHILD".equals(role)) {
					if (referer != null && referer.contains(TransitionTargetPageNameKeyword.LOGIN_CHILD)) {
						target = TransitionTargetPageNameKeyword.CHILD_HOME;
					} else {
						String errorTarget = TransitionTargetPageNameKeyword.LOGIN_CHILD + "?error";
						if (referer != null && referer.contains(TransitionTargetPageNameKeyword.LOGIN_ADMIN)) {
							errorTarget = TransitionTargetPageNameKeyword.LOGIN_ADMIN + "?error";
						} else if (referer != null && referer.contains(TransitionTargetPageNameKeyword.LOGIN_PARENT)) {
							errorTarget = TransitionTargetPageNameKeyword.LOGIN_PARENT + "?error";
						}
						response.sendRedirect(request.getContextPath() + errorTarget);
						return;
					}
				}
			}
			response.sendRedirect(request.getContextPath() + target);
		};
	}

	private AuthenticationFailureHandler failureHandler() {
		return (request, response, exception) -> {
			String referer = request.getHeader("Referer");
			String target = TransitionTargetPageNameKeyword.LOGIN_PARENT + "?error";

			if (referer != null) {
				if (referer.contains(TransitionTargetPageNameKeyword.LOGIN_ADMIN)) {
					target = TransitionTargetPageNameKeyword.LOGIN_ADMIN + "?error";
				} else if (referer.contains(TransitionTargetPageNameKeyword.LOGIN_CHILD)) {
					target = TransitionTargetPageNameKeyword.LOGIN_CHILD + "?error";
				}
			}

			response.sendRedirect(request.getContextPath() + target);
		};
	}
}