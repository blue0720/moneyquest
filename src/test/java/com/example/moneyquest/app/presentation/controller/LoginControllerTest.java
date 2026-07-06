package com.example.moneyquest.app.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * LoginController の単体テスト。
 * 画面遷移、およびログアウト時のセッション有無による分岐(例外を起こさないこと)を確認する。
 */
class LoginControllerTest {

	private final LoginController controller = new LoginController();

	@Test
	@DisplayName("タイトル画面はTITLE_HTMLを返す")
	void title_returnsTitleHtml() {
		assertThat(controller.title()).isEqualTo(TransitionTargetPageNameKeyword.TITLE_HTML);
	}

	@Test
	@DisplayName("保護者・子供・管理者ログイン画面はいずれも共通のLOGIN_HTMLを返す")
	void loginPages_returnCommonLoginHtml() {
		assertThat(controller.parentLogin()).isEqualTo(TransitionTargetPageNameKeyword.LOGIN_HTML);
		assertThat(controller.childLogin()).isEqualTo(TransitionTargetPageNameKeyword.LOGIN_HTML);
		assertThat(controller.adminLogin()).isEqualTo(TransitionTargetPageNameKeyword.LOGIN_HTML);
	}

	@Test
	@DisplayName("管理者用セッションエラー画面はERROR_ADMIN_HTMLを返す")
	void adminError_returnsAdminErrorHtml() {
		assertThat(controller.adminError()).isEqualTo(TransitionTargetPageNameKeyword.ERROR_ADMIN_HTML);
	}

	@Test
	@DisplayName("セッションが存在する場合はログアウト時に無効化されてタイトルへリダイレクトする")
	void logout_withSession_invalidatesSession() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpSession session = mock(HttpSession.class);
		when(request.getSession(false)).thenReturn(session);

		String result = controller.logout(request);

		verify(session).invalidate();
		assertThat(result).isEqualTo("redirect:/");
	}

	@Test
	@DisplayName("セッションが存在しない場合でも例外を投げずにタイトルへリダイレクトする")
	void logout_withoutSession_doesNotThrow() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getSession(false)).thenReturn(null);

		String result = controller.logout(request);

		assertThat(result).isEqualTo("redirect:/");
		verify(request, never()).getSession(true);
	}
}
