package com.example.moneyquest.app.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.example.moneyquest.app.presentation.form.QuestSendForm;

/**
 * QuestController の単体テスト。
 * 保護者・子供双方のクエスト操作と、コントローラレベルの
 * IllegalArgumentException ハンドラを重点的に確認する。
 */
@ExtendWith(MockitoExtension.class)
class QuestControllerTest {

	@Mock
	private QuestService questService;
	@Mock
	private UserService userService;
	@Mock
	private SpendingService spendingService;
	@Mock
	private CharacterService characterService;
	@Mock
	private IncomeExpenseService incomeExpenseService;
	@Mock
	private QuestTemplateService questTemplateService;
	@Mock
	private BadgeService badgeService;

	@InjectMocks
	private QuestController controller;

	private static final Integer PARENT_USER_ID = 1;
	private static final Integer CHILD_USER_ID = 10;

	private CustomUserDetails parentLogin;
	private CustomUserDetails childLogin;
	private Model model;

	@BeforeEach
	void setUp() {
		UserEntity parent = new UserEntity();
		parent.setUserId(PARENT_USER_ID);
		parentLogin = new CustomUserDetails(parent);

		UserEntity child = new UserEntity();
		child.setUserId(CHILD_USER_ID);
		child.setUserName("子供太郎");
		childLogin = new CustomUserDetails(child);

		model = new ExtendedModelMap();
	}

	@Test
	@DisplayName("questSendFormは新規のQuestSendFormを返す")
	void questSendForm_returnsNewForm() {
		assertThat(controller.questSendForm()).isInstanceOf(QuestSendForm.class);
	}

	@Nested
	@DisplayName("showQuests")
	class ShowQuests {

		@Test
		@DisplayName("保護者に紐づく子供全員分のクエストを集約して表示する")
		void showQuests_aggregatesAllChildren() {
			UserDto child = new UserDto();
			child.setUserId(CHILD_USER_ID);
			child.setAuthority(CustomUserDetails.AUTHORITY_CHILD);

			when(userService.getChildrenByParentId(PARENT_USER_ID)).thenReturn(List.of(child));
			when(questService.getQuestsByChildAndStatuses(eq(CHILD_USER_ID), any())).thenReturn(List.of());
			when(questService.getApprovalList()).thenReturn(List.of());
			when(spendingService.getPendingLimits(PARENT_USER_ID)).thenReturn(List.of());
			when(questTemplateService.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of());

			BindingResult bindingResult = new BeanPropertyBindingResult(new QuestSendForm(), "questSendForm");

			String view = controller.showQuests(parentLogin, new QuestSendForm(), bindingResult, model);

			assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.PARENT_HOME_HTML);
			assertThat(model.getAttribute("activeTab")).isEqualTo("quest");
		}
	}

	@Nested
	@DisplayName("createQuest")
	class CreateQuest {

		@Test
		@DisplayName("入力エラーがある場合はサービスを呼ばずに保護者ホームをエラー付きで再表示する")
		void createQuest_validationError_skipsService() {
			QuestSendForm form = new QuestSendForm();
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "questSendForm");
			bindingResult.reject("error");

			when(userService.getChildrenByParentId(PARENT_USER_ID)).thenReturn(List.of());
			when(questService.getApprovalList()).thenReturn(List.of());
			when(spendingService.getPendingLimits(PARENT_USER_ID)).thenReturn(List.of());
			when(questTemplateService.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of());

			String view = controller.createQuest(parentLogin, form, bindingResult, model);

			assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.PARENT_HOME_HTML);
			verify(questService, never()).createQuest(any(), any());
		}

		@Test
		@DisplayName("childUserIdが0(全員選択)の場合は子供全員分クエストを作成する")
		void createQuest_allChildren_createsForEach() {
			UserDto child1 = new UserDto();
			child1.setUserId(11);
			child1.setAuthority(CustomUserDetails.AUTHORITY_CHILD);
			UserDto child2 = new UserDto();
			child2.setUserId(12);
			child2.setAuthority(CustomUserDetails.AUTHORITY_CHILD);

			QuestSendForm form = new QuestSendForm();
			form.setChildUserId(0);
			form.setTitle("お手伝い");
			form.setRewardAmount(100);
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "questSendForm");

			when(userService.getChildrenByParentId(PARENT_USER_ID)).thenReturn(List.of(child1, child2));

			String view = controller.createQuest(parentLogin, form, bindingResult, model);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_QUESTS);
			verify(questService, times(2)).createQuest(any(QuestSendForm.class), eq(PARENT_USER_ID));
		}

		@Test
		@DisplayName("特定の子供が指定された場合はその子供のみクエストを作成する")
		void createQuest_singleChild_createsOnce() {
			QuestSendForm form = new QuestSendForm();
			form.setChildUserId(CHILD_USER_ID);
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "questSendForm");

			when(userService.getChildrenByParentId(PARENT_USER_ID)).thenReturn(List.of());

			String view = controller.createQuest(parentLogin, form, bindingResult, model);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_QUESTS);
			verify(questService, times(1)).createQuest(form, PARENT_USER_ID);
			verify(questTemplateService, never()).addTemplate(any(), any());
		}

		@Test
		@DisplayName("saveAsTemplateがtrueの場合はテンプレートも保存される")
		void createQuest_saveAsTemplate_addsTemplate() {
			QuestSendForm form = new QuestSendForm();
			form.setChildUserId(CHILD_USER_ID);
			form.setSaveAsTemplate(true);
			form.setTitle("お手伝い");
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "questSendForm");

			when(userService.getChildrenByParentId(PARENT_USER_ID)).thenReturn(List.of());

			controller.createQuest(parentLogin, form, bindingResult, model);

			verify(questTemplateService, times(1)).addTemplate(eq(PARENT_USER_ID), any());
		}
	}

	@Nested
	@DisplayName("updateQuest")
	class UpdateQuest {

		@Test
		@DisplayName("入力エラーがある場合はサービスを呼ばずリダイレクトする")
		void updateQuest_validationError_skipsService() {
			QuestSendForm form = new QuestSendForm();
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "questSendForm");
			bindingResult.reject("error");

			when(questService.getQuestsByChild(PARENT_USER_ID)).thenReturn(List.of());
			when(questService.getApprovalList()).thenReturn(List.of());
			when(spendingService.getPendingLimits(PARENT_USER_ID)).thenReturn(List.of());
			when(questTemplateService.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of());

			String view = controller.updateQuest(1, parentLogin, form, bindingResult, model);

			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_QUESTS);
			verify(questService, never()).updateQuest(any(), any());
		}

		@Test
		@DisplayName("正常な場合はquestIdをフォームに設定してサービスを呼ぶ")
		void updateQuest_success() {
			QuestSendForm form = new QuestSendForm();
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "questSendForm");

			String view = controller.updateQuest(123, parentLogin, form, bindingResult, model);

			assertThat(form.getQuestId()).isEqualTo(123);
			assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_QUESTS);
			verify(questService, times(1)).updateQuest(form, PARENT_USER_ID);
		}
	}

	@Test
	@DisplayName("deleteQuestは削除後、クエスト一覧へリダイレクトする")
	void deleteQuest_redirectsToQuestList() {
		String view = controller.deleteQuest(1, parentLogin);

		assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_QUESTS);
		verify(questService, times(1)).deleteQuest(1, PARENT_USER_ID);
	}

	@Test
	@DisplayName("approveQuestは承認後、承認待ち一覧へリダイレクトする")
	void approveQuest_redirectsToApprovals() {
		QuestSendForm form = new QuestSendForm();

		String view = controller.approveQuest(parentLogin, 1, form);

		assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_APPROVALS);
		verify(questService, times(1)).approveQuest(form, 1, parentLogin.getUser());
	}

	@Test
	@DisplayName("rejectQuestは却下後、承認待ち一覧へリダイレクトする")
	void rejectQuest_redirectsToApprovals() {
		String view = controller.rejectQuest(1, parentLogin);

		assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.PARENT_APPROVALS);
		verify(questService, times(1)).rejectQuest(1, PARENT_USER_ID);
	}

	@Nested
	@DisplayName("showChildQuests")
	class ShowChildQuests {

		@Test
		@DisplayName("バインドエラーがない場合は閲覧済みマーキングが実行される")
		void showChildQuests_marksViewedWhenNoErrors() {
			QuestSendForm form = new QuestSendForm();
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "questSendForm");

			when(questService.getQuestsByChildAndStatuses(eq(CHILD_USER_ID), any())).thenReturn(List.of());
			when(incomeExpenseService.getRecords(CHILD_USER_ID)).thenReturn(List.of());
			when(questService.getApprovalList()).thenReturn(List.of());
			when(spendingService.getPendingLimits(CHILD_USER_ID)).thenReturn(List.of());
			when(questTemplateService.findByParentUserId(CHILD_USER_ID)).thenReturn(List.of());
			when(characterService.getCharacter(CHILD_USER_ID)).thenReturn(null);
			when(badgeService.getBadges(CHILD_USER_ID)).thenReturn(List.of());
			when(spendingService.getCurrentLimit(CHILD_USER_ID)).thenReturn(null);
			when(spendingService.getChildLimitHistory(CHILD_USER_ID)).thenReturn(List.of());

			String view = controller.showChildQuests(form, childLogin, bindingResult, model);

			assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.CHILD_HOME_HTML);
			verify(questService, times(1)).markQuestsViewed(CHILD_USER_ID);
		}

		@Test
		@DisplayName("バインドエラーがある場合は閲覧済みマーキングをスキップする")
		void showChildQuests_skipsMarkingWhenErrors() {
			QuestSendForm form = new QuestSendForm();
			BindingResult bindingResult = new BeanPropertyBindingResult(form, "questSendForm");
			bindingResult.reject("error");

			when(questService.getQuestsByChildAndStatuses(eq(CHILD_USER_ID), any())).thenReturn(List.of());
			when(incomeExpenseService.getRecords(CHILD_USER_ID)).thenReturn(List.of());
			when(questService.getApprovalList()).thenReturn(List.of());
			when(spendingService.getPendingLimits(CHILD_USER_ID)).thenReturn(List.of());
			when(questTemplateService.findByParentUserId(CHILD_USER_ID)).thenReturn(List.of());
			when(characterService.getCharacter(CHILD_USER_ID)).thenReturn(null);
			when(badgeService.getBadges(CHILD_USER_ID)).thenReturn(List.of());
			when(spendingService.getCurrentLimit(CHILD_USER_ID)).thenReturn(null);
			when(spendingService.getChildLimitHistory(CHILD_USER_ID)).thenReturn(List.of());

			controller.showChildQuests(form, childLogin, bindingResult, model);

			verify(questService, never()).markQuestsViewed(anyInt());
		}
	}

	@Test
	@DisplayName("requestCompleteは完了申請後、クエスト一覧へリダイレクトする")
	void requestComplete_redirectsToQuestList() {
		String view = controller.requestComplete(1, childLogin);

		assertThat(view).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.CHILD_QUESTS);
		verify(questService, times(1)).requestComplete(1, CHILD_USER_ID);
	}

	@Test
	@DisplayName("IllegalArgumentExceptionハンドラはエラー画面のビュー名を返す")
	void handleIllegalArgumentException_returnsErrorView() {
		String view = controller.handleIllegalArgumentException(new IllegalArgumentException("test"));

		assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.ERROR_HTML);
	}
}
