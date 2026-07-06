package com.example.moneyquest.app.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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

import com.example.moneyquest.app.domain.service.CustomUserDetails;
import com.example.moneyquest.app.domain.service.QuestService;
import com.example.moneyquest.app.domain.service.QuestTemplateService;
import com.example.moneyquest.app.domain.service.SpendingService;
import com.example.moneyquest.app.domain.service.UserService;
import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword;
import com.example.moneyquest.app.presentation.form.UserForm;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * UserController の単体テスト。
 * パスワード確認チェック、UserService からの IllegalStateException/IllegalArgumentException
 * ハンドリング(特に修正した deleteUser の例外処理)を重点的に確認する。
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

	@Mock
	private UserService userService;
	@Mock
	private SpendingService spendingService;
	@Mock
	private QuestService questService;
	@Mock
	private QuestTemplateService questTemplateService;

	@InjectMocks
	private UserController controller;

	private static final Integer PARENT_USER_ID = 1;

	private CustomUserDetails parentLogin;
	private Model model;

	@BeforeEach
	void setUp() {
		UserEntity parent = new UserEntity();
		parent.setUserId(PARENT_USER_ID);
		parentLogin = new CustomUserDetails(parent);
		model = new ExtendedModelMap();
	}

	private UserForm formWithMatchingPasswords(String password) {
		UserForm form = new UserForm();
		form.setUserName("名前");
		form.setMailAddress("test@example.com");
		form.setPassword(password);
		form.setPasswordConfirm(password);
		return form;
	}

	private void stubFamilyModelDependencies() {
		when(questService.getApprovalList()).thenReturn(List.of());
		when(spendingService.getPendingLimits(PARENT_USER_ID)).thenReturn(List.of());
		when(questTemplateService.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of());
		when(userService.getFamilyByParentId(PARENT_USER_ID)).thenReturn(List.of());
	}

	@Nested
	@DisplayName("createParent")
	class CreateParent {

		@Test
		@DisplayName("パスワード確認が一致しない場合はサービスを呼ばず登録画面を再表示する")
		void createParent_passwordMismatch_rerendersForm() {
			UserForm form = formWithMatchingPasswords("pass1234");
			form.setPasswordConfirm("different1");
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");

			String view = controller.createParent(form, bindingResult, model);

			assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.REGISTER_PARENT_HTML);
			assertThat(bindingResult.hasErrors()).isTrue();
		}

		@Test
		@DisplayName("メール重複などIllegalStateExceptionの場合はエラーメッセージ付きで再表示する")
		void createParent_duplicatedMail_showsError() {
			UserForm form = formWithMatchingPasswords("pass1234");
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");

			doThrow(new IllegalStateException("このメールアドレスはすでに使用されています"))
					.when(userService).createUser(any(), any());

			String view = controller.createParent(form, bindingResult, model);

			assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.REGISTER_PARENT_HTML);
			assertThat(model.getAttribute("errorMessage")).isEqualTo("このメールアドレスはすでに使用されています");
		}

		@Test
		@DisplayName("正常な場合は登録後ログイン画面へリダイレクトする")
		void createParent_success() {
			UserForm form = formWithMatchingPasswords("pass1234");
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");

			String view = controller.createParent(form, bindingResult, model);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.LOGIN_PARENT);
			verify(userService, times(1)).createUser(CustomUserDetails.AUTHORITY_PARENT, form);
		}
	}

	@Nested
	@DisplayName("deleteUser")
	class DeleteUser {

		@Test
		@DisplayName("正常に削除できた場合(自分以外)は家族タブへリダイレクトする")
		void deleteUser_child_success() {
			HttpServletRequest request = mock(HttpServletRequest.class);

			String view = controller.deleteUser(99, parentLogin, request);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_FAMILY);
			verify(userService, times(1)).deleteFamilyUser(PARENT_USER_ID, 99);
		}

		@Test
		@DisplayName("自分自身を削除できた場合はセッションを無効化してタイトルへリダイレクトする")
		void deleteUser_self_invalidatesSessionAndRedirectsToTitle() {
			HttpServletRequest request = mock(HttpServletRequest.class);
			HttpSession session = mock(HttpSession.class);
			when(request.getSession(false)).thenReturn(session);

			String view = controller.deleteUser(PARENT_USER_ID, parentLogin, request);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.TITLE);
			verify(session).invalidate();
		}

		@Test
		@DisplayName("対象が見つからない場合(IllegalArgumentException)はエラー画面へリダイレクトし例外を伝播させない")
		void deleteUser_notFound_redirectsToError() {
			HttpServletRequest request = mock(HttpServletRequest.class);
			doThrow(new IllegalArgumentException("対象のアカウントが見つかりません"))
					.when(userService).deleteFamilyUser(any(), any());

			String view = controller.deleteUser(999, parentLogin, request);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.ERROR);
		}

		@Test
		@DisplayName("削除権限がない場合(IllegalStateException)はエラー画面へリダイレクトし例外を伝播させない")
		void deleteUser_notOwned_redirectsToError() {
			HttpServletRequest request = mock(HttpServletRequest.class);
			doThrow(new IllegalStateException("このアカウントは削除できません"))
					.when(userService).deleteFamilyUser(any(), any());

			String view = controller.deleteUser(55, parentLogin, request);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.ERROR);
		}
	}

	@Nested
	@DisplayName("updateUser")
	class UpdateUser {

		@Test
		@DisplayName("IllegalStateExceptionの場合はエラーメッセージ付きで家族タブモデルを再表示する")
		void updateUser_duplicatedMail_showsError() {
			UserForm form = formWithMatchingPasswords("");
			form.setPassword("");
			form.setPasswordConfirm("");
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");

			stubFamilyModelDependencies();
			doThrow(new IllegalStateException("このメールアドレスはすでに使用されています"))
					.when(userService).updateUser(any(), any());

			String view = controller.updateUser(parentLogin, form, bindingResult, model);

			assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.PARENT_HOME_HTML);
			assertThat(model.getAttribute("errorMessage")).isEqualTo("このメールアドレスはすでに使用されています");
		}

		@Test
		@DisplayName("正常な場合は家族タブへリダイレクトする")
		void updateUser_success() {
			UserForm form = formWithMatchingPasswords("");
			form.setPassword("");
			form.setPasswordConfirm("");
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");

			String view = controller.updateUser(parentLogin, form, bindingResult, model);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_FAMILY);
		}
	}

	@Nested
	@DisplayName("createChild")
	class CreateChild {

		@Test
		@DisplayName("IllegalStateExceptionの場合はエラーメッセージ付きで家族タブモデルを再表示する")
		void createChild_duplicatedMail_showsError() {
			UserForm form = formWithMatchingPasswords("pass1234");
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");

			stubFamilyModelDependencies();
			doThrow(new IllegalStateException("このメールアドレスはすでに使用されています"))
					.when(userService).createChildUser(any(), any());

			String view = controller.createChild(parentLogin, form, bindingResult, model);

			assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.PARENT_HOME_HTML);
			assertThat(model.getAttribute("errorMessage")).isNotNull();
		}

		@Test
		@DisplayName("正常な場合は家族タブへリダイレクトする")
		void createChild_success() {
			UserForm form = formWithMatchingPasswords("pass1234");
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");

			String view = controller.createChild(parentLogin, form, bindingResult, model);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_FAMILY);
			verify(userService, times(1)).createChildUser(PARENT_USER_ID, form);
		}
	}

	@Nested
	@DisplayName("updateAdmin")
	class UpdateAdmin {

		@Test
		@DisplayName("自分以外の管理者を編集しようとした場合は管理者エラー画面へリダイレクトする")
		void updateAdmin_otherAdmin_redirectsToAdminError() {
			UserForm form = formWithMatchingPasswords("");
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");

			String view = controller.updateAdmin(parentLogin, 999, form, bindingResult, model);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.ERROR_ADMIN);
			verify(userService, times(0)).updateUser(any(), any());
		}
	}

	@Nested
	@DisplayName("deleteAdmin")
	class DeleteAdmin {

		@Test
		@DisplayName("自分自身を削除しようとした場合は管理者エラー画面へリダイレクトする")
		void deleteAdmin_self_redirectsToAdminError() {
			String view = controller.deleteAdmin(PARENT_USER_ID, parentLogin);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.ERROR_ADMIN);
			verify(userService, times(0)).deleteUser(any());
		}

		@Test
		@DisplayName("他の管理者を削除する場合は管理者一覧へリダイレクトする")
		void deleteAdmin_other_redirectsToAdminList() {
			String view = controller.deleteAdmin(999, parentLogin);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.ADMIN_ACCOUNTS);
			verify(userService, times(1)).deleteUser(999);
		}
	}
}
