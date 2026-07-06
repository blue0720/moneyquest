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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.example.moneyquest.app.domain.service.BadgeService;
import com.example.moneyquest.app.domain.service.CharacterService;
import com.example.moneyquest.app.domain.service.CustomUserDetails;
import com.example.moneyquest.app.domain.service.GraphService;
import com.example.moneyquest.app.domain.service.IncomeExpenseService;
import com.example.moneyquest.app.domain.service.QuestService;
import com.example.moneyquest.app.domain.service.QuestTemplateService;
import com.example.moneyquest.app.domain.service.SpendingService;
import com.example.moneyquest.app.domain.service.UserService;
import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.infra.repository.UserRepository;
import com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword;
import com.example.moneyquest.app.presentation.form.IncomeExpenseForm;

/**
 * IncomeExpenseController の単体テスト。
 * サービス層からの SecurityException / IllegalArgumentException を
 * コントローラがどう握りつぶし・リダイレクトするかを重点的に確認する。
 */
@ExtendWith(MockitoExtension.class)
class IncomeExpenseControllerTest {

	@Mock
	private IncomeExpenseService incomeExpenseService;
	@Mock
	private GraphService graphService;
	@Mock
	private CharacterService characterService;
	@Mock
	private SpendingService spendingService;
	@Mock
	private UserService userService;
	@Mock
	private QuestService questService;
	@Mock
	private QuestTemplateService questTemplateService;
	@Mock
	private UserRepository userRepository;
	@Mock
	private BadgeService badgeService;

	@InjectMocks
	private IncomeExpenseController controller;

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
		child.setAuthority(CustomUserDetails.AUTHORITY_CHILD);
		childLogin = new CustomUserDetails(child);

		UserEntity parent = new UserEntity();
		parent.setUserId(PARENT_USER_ID);
		parent.setAuthority(CustomUserDetails.AUTHORITY_PARENT);
		parentLogin = new CustomUserDetails(parent);

		model = new ExtendedModelMap();
	}

	@Nested
	@DisplayName("incomeExpenseList")
	class IncomeExpenseList {

		@Test
		@DisplayName("子供がアクセスした場合は子供ホームを返す")
		void incomeExpenseList_child_returnsChildHome() {
			when(characterService.getCharacter(CHILD_USER_ID)).thenReturn(null);
			when(spendingService.getCurrentLimit(CHILD_USER_ID)).thenReturn(null);
			when(incomeExpenseService.getRecords(CHILD_USER_ID)).thenReturn(List.of());
			when(badgeService.getBadges(CHILD_USER_ID)).thenReturn(List.of());

			String view = controller.incomeExpenseList(childLogin, model);

			assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.CHILD_HOME_HTML);
		}

		@Test
		@DisplayName("保護者がアクセスした場合は保護者ホームを返す")
		void incomeExpenseList_parent_returnsParentHome() {
			when(userRepository.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of());
			when(questService.getApprovalList()).thenReturn(List.of());
			when(spendingService.getPendingLimits(PARENT_USER_ID)).thenReturn(List.of());
			when(questTemplateService.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of());
			when(incomeExpenseService.getParentIncomeRecords(PARENT_USER_ID)).thenReturn(List.of());

			String view = controller.incomeExpenseList(parentLogin, model);

			assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.PARENT_HOME_HTML);
		}
	}

	@Nested
	@DisplayName("createIncomeExpense")
	class CreateIncomeExpense {

		@Test
		@DisplayName("入力エラーがある場合は登録処理を呼ばずリダイレクトする")
		void createIncomeExpense_validationError_skipsService() {
			IncomeExpenseForm form = new IncomeExpenseForm();
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "incomeExpenseForm");
			bindingResult.reject("error");

			when(incomeExpenseService.getExpenseRecords(CHILD_USER_ID)).thenReturn(List.of());
			when(characterService.getCharacter(CHILD_USER_ID)).thenReturn(null);
			when(spendingService.getCurrentLimit(CHILD_USER_ID)).thenReturn(null);
			when(incomeExpenseService.getRecords(CHILD_USER_ID)).thenReturn(List.of());
			when(badgeService.getBadges(CHILD_USER_ID)).thenReturn(List.of());

			String view = controller.createIncomeExpense(childLogin, form, bindingResult, model);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.CHILD_RECORDS);
			verify(incomeExpenseService, org.mockito.Mockito.never()).createRecord(any(), any(), any());
		}

		@Test
		@DisplayName("サービスがSecurityExceptionを投げた場合はエラー画面へリダイレクトする")
		void createIncomeExpense_serviceThrowsSecurityException_redirectsToError() {
			IncomeExpenseForm form = new IncomeExpenseForm();
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "incomeExpenseForm");

			doThrow(new SecurityException("権限がありません"))
					.when(incomeExpenseService).createRecord(any(), any(), any());

			String view = controller.createIncomeExpense(childLogin, form, bindingResult, model);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.ERROR);
		}

		@Test
		@DisplayName("正常に登録できた場合は子供の記録一覧へリダイレクトする")
		void createIncomeExpense_success_child() {
			IncomeExpenseForm form = new IncomeExpenseForm();
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "incomeExpenseForm");

			String view = controller.createIncomeExpense(childLogin, form, bindingResult, model);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.CHILD_RECORDS);
			verify(incomeExpenseService, times(1)).createRecord(form, childLogin.getUser(), null);
		}

		@Test
		@DisplayName("保護者が登録した場合は保護者の収入一覧へリダイレクトする")
		void createIncomeExpense_success_parent() {
			IncomeExpenseForm form = new IncomeExpenseForm();
			form.setChildUserId(CHILD_USER_ID);
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "incomeExpenseForm");

			String view = controller.createIncomeExpense(parentLogin, form, bindingResult, model);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_INCOME);
			verify(incomeExpenseService, times(1)).createRecord(form, parentLogin.getUser(), CHILD_USER_ID);
		}
	}

	@Nested
	@DisplayName("updateIncomeExpense")
	class UpdateIncomeExpense {

		@Test
		@DisplayName("サービスがIllegalArgumentExceptionを投げた場合はフラッシュメッセージ付きでリダイレクトする")
		void updateIncomeExpense_illegalArgument_redirectsWithFlashMessage() {
			IncomeExpenseForm form = new IncomeExpenseForm();
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "incomeExpenseForm");
			RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

			doThrow(new IllegalArgumentException("対象が見つかりません"))
					.when(incomeExpenseService).updateRecord(any(), any());

			String view = controller.updateIncomeExpense(childLogin, 1, form, bindingResult, model, redirectAttributes);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.CHILD_RECORDS);
			assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isEqualTo("対象が見つかりません");
		}

		@Test
		@DisplayName("サービスがSecurityExceptionを投げた場合はエラー画面へリダイレクトする")
		void updateIncomeExpense_securityException_redirectsToError() {
			IncomeExpenseForm form = new IncomeExpenseForm();
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "incomeExpenseForm");
			RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

			doThrow(new SecurityException("権限がありません"))
					.when(incomeExpenseService).updateRecord(any(), any());

			String view = controller.updateIncomeExpense(childLogin, 1, form, bindingResult, model, redirectAttributes);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.ERROR);
		}

		@Test
		@DisplayName("正常に更新できた場合はIDをフォームにセットして記録一覧へリダイレクトする")
		void updateIncomeExpense_success() {
			IncomeExpenseForm form = new IncomeExpenseForm();
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "incomeExpenseForm");
			RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

			String view = controller.updateIncomeExpense(childLogin, 5, form, bindingResult, model, redirectAttributes);

			assertThat(form.getIncomeExpenseId()).isEqualTo(5);
			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.CHILD_RECORDS);
		}
	}

	@Nested
	@DisplayName("deleteIncomeExpense")
	class DeleteIncomeExpense {

		@Test
		@DisplayName("サービスがSecurityExceptionを投げた場合はエラー画面へリダイレクトする")
		void deleteIncomeExpense_securityException_redirectsToError() {
			doThrow(new SecurityException("権限がありません"))
					.when(incomeExpenseService).deleteRecord(any(), any());

			String view = controller.deleteIncomeExpense(childLogin, 1);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.ERROR);
		}

		@Test
		@DisplayName("正常に削除できた場合は保護者収入一覧へリダイレクトする")
		void deleteIncomeExpense_success_parent() {
			String view = controller.deleteIncomeExpense(parentLogin, 1);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_INCOME);
			verify(incomeExpenseService, times(1)).deleteRecord(1, parentLogin.getUser());
		}
	}
}
