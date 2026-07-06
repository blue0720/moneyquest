package com.example.moneyquest.app.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

import com.example.moneyquest.app.domain.model.UserDto;
import com.example.moneyquest.app.domain.service.CustomUserDetails;
import com.example.moneyquest.app.domain.service.QuestService;
import com.example.moneyquest.app.domain.service.QuestTemplateService;
import com.example.moneyquest.app.domain.service.SpendingService;
import com.example.moneyquest.app.domain.service.UserService;
import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword;
import com.example.moneyquest.app.presentation.form.QuestSendForm;
import com.example.moneyquest.app.presentation.form.QuestTemplateForm;

/**
 * QuestTemplateController の単体テスト。
 * バリデーションエラー時の再表示、およびテンプレートからの一括クエスト作成を確認する。
 */
@ExtendWith(MockitoExtension.class)
class QuestTemplateControllerTest {

	@Mock
	private QuestTemplateService questTemplateService;
	@Mock
	private UserService userService;
	@Mock
	private QuestService questService;
	@Mock
	private SpendingService spendingService;

	@InjectMocks
	private QuestTemplateController controller;

	private static final Integer PARENT_USER_ID = 1;

	private CustomUserDetails parentLogin;
	private Model model;

	@BeforeEach
	void setUp() {
		UserEntity parent = new UserEntity();
		parent.setUserId(PARENT_USER_ID);
		parent.setUserName("親太郎");
		parentLogin = new CustomUserDetails(parent);
		model = new ExtendedModelMap();
	}

	private void stubShowTemplatesDependencies() {
		when(questTemplateService.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of());
		when(userService.getFamilyByParentId(PARENT_USER_ID)).thenReturn(List.of());
		when(questService.getApprovalList()).thenReturn(List.of());
		when(spendingService.getPendingLimits(PARENT_USER_ID)).thenReturn(List.of());
	}

	@Test
	@DisplayName("showTemplatesはテンプレート一覧を表示する")
	void showTemplates_returnsParentHome() {
		stubShowTemplatesDependencies();

		String view = controller.showTemplates(parentLogin, model);

		assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.PARENT_HOME_HTML);
		assertThat(model.getAttribute("activeTab")).isEqualTo("template");
	}

	@Nested
	@DisplayName("addTemplate")
	class AddTemplate {

		@Test
		@DisplayName("入力エラーがある場合はサービスを呼ばず一覧を再表示する")
		void addTemplate_validationError_skipsService() {
			QuestTemplateForm form = new QuestTemplateForm();
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "questTemplateForm");
			bindingResult.reject("error");

			stubShowTemplatesDependencies();

			String view = controller.addTemplate(parentLogin, form, bindingResult, model);

			assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.PARENT_HOME_HTML);
			assertThat(model.getAttribute("templateErrorMessage")).isNotNull();
			verify(questTemplateService, never()).addTemplate(any(), any());
		}

		@Test
		@DisplayName("正常な場合はテンプレートを追加してリダイレクトする")
		void addTemplate_success() {
			QuestTemplateForm form = new QuestTemplateForm();
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "questTemplateForm");

			String view = controller.addTemplate(parentLogin, form, bindingResult, model);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_TEMPLATES);
			verify(questTemplateService, times(1)).addTemplate(PARENT_USER_ID, form);
		}
	}

	@Nested
	@DisplayName("updateTemplate")
	class UpdateTemplate {

		@Test
		@DisplayName("正常な場合はidをフォームに設定して更新する")
		void updateTemplate_success() {
			QuestTemplateForm form = new QuestTemplateForm();
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "questTemplateForm");

			String view = controller.updateTemplate(parentLogin, 5, form, bindingResult, model);

			assertThat(form.getQuestTemplateId()).isEqualTo(5);
			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_TEMPLATES);
			verify(questTemplateService, times(1)).updateTemplate(PARENT_USER_ID, form);
		}

		@Test
		@DisplayName("入力エラーがある場合は一覧を再表示する")
		void updateTemplate_validationError_skipsService() {
			QuestTemplateForm form = new QuestTemplateForm();
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "questTemplateForm");
			bindingResult.reject("error");

			stubShowTemplatesDependencies();

			String view = controller.updateTemplate(parentLogin, 5, form, bindingResult, model);

			assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.PARENT_HOME_HTML);
			verify(questTemplateService, never()).updateTemplate(any(), any());
		}
	}

	@Test
	@DisplayName("deleteTemplateは削除後、テンプレート一覧へリダイレクトする")
	void deleteTemplate_redirects() {
		String view = controller.deleteTemplate(parentLogin, 5);

		assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_TEMPLATES);
		verify(questTemplateService, times(1)).deleteTemplate(PARENT_USER_ID, 5);
	}

	@Nested
	@DisplayName("addQuestFromTemplate")
	class AddQuestFromTemplate {

		@Test
		@DisplayName("childUserIdが0(全員選択)の場合は子供全員分クエストを作成する")
		void addQuestFromTemplate_allChildren() {
			UserDto child1 = new UserDto();
			child1.setUserId(11);
			child1.setAuthority(CustomUserDetails.AUTHORITY_CHILD);
			UserDto child2 = new UserDto();
			child2.setUserId(12);
			child2.setAuthority(CustomUserDetails.AUTHORITY_CHILD);

			QuestSendForm form = new QuestSendForm();
			form.setChildUserId(0);
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "questSendForm");

			when(userService.getFamilyByParentId(PARENT_USER_ID)).thenReturn(List.of(child1, child2));

			String view = controller.addQuestFromTemplate(parentLogin, form, bindingResult, model);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_TEMPLATES);
			verify(questService, times(2)).createQuest(any(QuestSendForm.class), eq(PARENT_USER_ID));
		}

		@Test
		@DisplayName("特定の子供が指定された場合はその子供のみクエストを作成する")
		void addQuestFromTemplate_singleChild() {
			QuestSendForm form = new QuestSendForm();
			form.setChildUserId(11);
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "questSendForm");

			String view = controller.addQuestFromTemplate(parentLogin, form, bindingResult, model);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_TEMPLATES);
			verify(questService, times(1)).createQuest(form, PARENT_USER_ID);
		}

		@Test
		@DisplayName("入力エラーがある場合はサービスを呼ばずテンプレート一覧を再表示する")
		void addQuestFromTemplate_validationError_skipsService() {
			QuestSendForm form = new QuestSendForm();
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "questSendForm");
			bindingResult.reject("error");

			stubShowTemplatesDependencies();

			String view = controller.addQuestFromTemplate(parentLogin, form, bindingResult, model);

			assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.PARENT_HOME_HTML);
			verify(questService, never()).createQuest(any(), any());
		}
	}
}
