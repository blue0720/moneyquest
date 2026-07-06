package com.example.moneyquest.app.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.example.moneyquest.app.domain.service.BadgeService;
import com.example.moneyquest.app.domain.service.CharacterService;
import com.example.moneyquest.app.domain.service.CustomUserDetails;
import com.example.moneyquest.app.domain.service.IncomeExpenseService;
import com.example.moneyquest.app.domain.service.QuestService;
import com.example.moneyquest.app.domain.service.QuestTemplateService;
import com.example.moneyquest.app.domain.service.SpendingService;
import com.example.moneyquest.app.domain.service.UserService;
import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword;
import com.example.moneyquest.app.presentation.form.SpendingLimitForm;

/**
 * SpendingLimitController の単体テスト。
 * approve/reject が SpendingService の SecurityException / IllegalArgumentException(対象未存在)
 * のどちらでもエラー画面へ正しくリダイレクトすることを重点的に確認する。
 */
@ExtendWith(MockitoExtension.class)
class SpendingLimitControllerTest {

	@Mock
	private SpendingService spendingService;
	@Mock
	private CharacterService characterService;
	@Mock
	private IncomeExpenseService incomeExpenseService;
	@Mock
	private UserService userService;
	@Mock
	private QuestService questService;
	@Mock
	private QuestTemplateService questTemplateService;
	@Mock
	private BadgeService badgeService;

	@InjectMocks
	private SpendingLimitController controller;

	private static final Integer CHILD_USER_ID = 10;
	private static final Integer PARENT_USER_ID = 1;

	private CustomUserDetails childLogin;
	private CustomUserDetails parentLogin;
	private Model model;

	@BeforeEach
	void setUp() {
		UserEntity child = new UserEntity();
		child.setUserId(CHILD_USER_ID);
		child.setUserName("子供太郎");
		childLogin = new CustomUserDetails(child);

		UserEntity parent = new UserEntity();
		parent.setUserId(PARENT_USER_ID);
		parentLogin = new CustomUserDetails(parent);

		model = new ExtendedModelMap();
	}

	@Test
	@DisplayName("showSpendingLimitsは子供ホームに必要な属性を設定する")
	void showSpendingLimits_setsAttributes() {
		when(characterService.getCharacter(CHILD_USER_ID)).thenReturn(null);
		when(badgeService.getBadges(CHILD_USER_ID)).thenReturn(List.of());
		when(spendingService.getCurrentLimit(CHILD_USER_ID)).thenReturn(null);
		when(spendingService.getChildLimitHistory(CHILD_USER_ID)).thenReturn(List.of());
		when(incomeExpenseService.getRecords(CHILD_USER_ID)).thenReturn(List.of());

		String view = controller.showSpendingLimits(childLogin, model);

		assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.CHILD_HOME_HTML);
		assertThat(model.getAttribute("activeTab")).isEqualTo("limit");
	}

	@Test
	@DisplayName("requestLimitは申請後、上限タブへリダイレクトする")
	void requestLimit_redirects() {
		SpendingLimitForm form = new SpendingLimitForm();
		form.setLimitAmount(3000);

		String view = controller.requestLimit(childLogin, form);

		assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.CHILD_LIMIT);
		verify(spendingService, times(1)).requestLimit(form, CHILD_USER_ID);
	}

	@Test
	@DisplayName("showPendingLimitsは承認待ち・履歴一覧を表示する")
	void showPendingLimits_setsAttributes() {
		when(questService.getApprovalList()).thenReturn(List.of());
		when(spendingService.getPendingLimits(PARENT_USER_ID)).thenReturn(List.of());
		when(questTemplateService.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of());
		when(spendingService.getParentLimitHistory(PARENT_USER_ID)).thenReturn(List.of());

		String view = controller.showPendingLimits(parentLogin, model);

		assertThat(view).isEqualTo("parent-home");
		assertThat(model.getAttribute("activeTab")).isEqualTo("limit");
	}

	@Nested
	@DisplayName("approveLimit")
	class ApproveLimit {

		@Test
		@DisplayName("正常な場合は承認後、上限承認タブへリダイレクトする")
		void approveLimit_success() {
			String view = controller.approveLimit(1, parentLogin);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_LIMIT);
			verify(spendingService, times(1)).approveLimit(1, PARENT_USER_ID);
		}

		@Test
		@DisplayName("SecurityException(権限なし)の場合はエラー画面へリダイレクトする")
		void approveLimit_securityException_redirectsToError() {
			doThrow(new SecurityException("権限がありません")).when(spendingService).approveLimit(any(), any());

			String view = controller.approveLimit(1, parentLogin);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.ERROR);
		}

		@Test
		@DisplayName("IllegalArgumentException(対象未存在)の場合もエラー画面へリダイレクトする")
		void approveLimit_illegalArgumentException_redirectsToError() {
			doThrow(new IllegalArgumentException("対象の支出上限申請が見つかりません。"))
					.when(spendingService).approveLimit(any(), any());

			String view = controller.approveLimit(999, parentLogin);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.ERROR);
		}
	}

	@Nested
	@DisplayName("rejectLimit")
	class RejectLimit {

		@Test
		@DisplayName("正常な場合は却下後、上限承認タブへリダイレクトする")
		void rejectLimit_success() {
			String view = controller.rejectLimit(1, parentLogin);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_LIMIT);
			verify(spendingService, times(1)).rejectLimit(1, PARENT_USER_ID);
		}

		@Test
		@DisplayName("SecurityException(権限なし)の場合はエラー画面へリダイレクトする")
		void rejectLimit_securityException_redirectsToError() {
			doThrow(new SecurityException("権限がありません")).when(spendingService).rejectLimit(any(), any());

			String view = controller.rejectLimit(1, parentLogin);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.ERROR);
		}

		@Test
		@DisplayName("IllegalArgumentException(対象未存在)の場合もエラー画面へリダイレクトする")
		void rejectLimit_illegalArgumentException_redirectsToError() {
			doThrow(new IllegalArgumentException("対象の支出上限申請が見つかりません。"))
					.when(spendingService).rejectLimit(any(), any());

			String view = controller.rejectLimit(999, parentLogin);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.ERROR);
		}
	}
}
