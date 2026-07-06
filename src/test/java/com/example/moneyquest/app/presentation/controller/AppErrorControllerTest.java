package com.example.moneyquest.app.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword;

import jakarta.servlet.http.HttpServletRequest;

/**
 * AppErrorController の単体テスト。
 * エラー発生元URLに応じた画面振り分け(管理者/一般)を確認する。
 */
@ExtendWith(MockitoExtension.class)
class AppErrorControllerTest {

	@Mock
	private HttpServletRequest request;

	private final AppErrorController controller = new AppErrorController();

	@Test
	@DisplayName("/admin配下で発生したエラーは管理者用エラー画面を返す")
	void handleError_adminUri_returnsAdminError() {
		when(request.getAttribute("jakarta.servlet.error.request_uri")).thenReturn("/admin/parents");

		String view = controller.handleError(request);

		assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.ERROR_ADMIN_HTML);
	}

	@Test
	@DisplayName("一般URLで発生したエラーは通常のエラー画面を返す")
	void handleError_normalUri_returnsGeneralError() {
		when(request.getAttribute("jakarta.servlet.error.request_uri")).thenReturn("/parent/home");

		String view = controller.handleError(request);

		assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.ERROR_HTML);
	}

	@Test
	@DisplayName("エラー発生元URLが取得できない場合も通常のエラー画面を返す")
	void handleError_nullUri_returnsGeneralError() {
		when(request.getAttribute("jakarta.servlet.error.request_uri")).thenReturn(null);

		String view = controller.handleError(request);

		assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.ERROR_HTML);
	}
}
