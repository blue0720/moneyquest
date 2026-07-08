package com.example.moneyquest.app.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.example.moneyquest.app.domain.model.CharacterDto;
import com.example.moneyquest.app.domain.model.IncomeExpenseDto;
import com.example.moneyquest.app.domain.model.UserDto;
import com.example.moneyquest.app.domain.service.BadgeService;
import com.example.moneyquest.app.domain.service.CharacterService;
import com.example.moneyquest.app.domain.service.CustomUserDetails;
import com.example.moneyquest.app.domain.service.IncomeExpenseService;
import com.example.moneyquest.app.domain.service.QuestService;
import com.example.moneyquest.app.domain.service.QuestTemplateService;
import com.example.moneyquest.app.domain.service.SpendingService;
import com.example.moneyquest.app.domain.service.UserService;
import com.example.moneyquest.app.infra.entity.QuestEntity;
import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword;
import com.example.moneyquest.app.presentation.form.CharacterForm;

/**
 * HomeController の単体テスト。
 * 子供ホーム・保護者ホーム・保護者収支タブの画面遷移とモデル属性を確認する。
 */
@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

	@Mock
	private CharacterService characterService;
	@Mock
	private IncomeExpenseService incomeExpenseService;
	@Mock
	private QuestService questService;
	@Mock
	private SpendingService spendingService;
	@Mock
	private QuestTemplateService questTemplateService;
	@Mock
	private UserService userService;
	@Mock
	private BadgeService badgeService;

	@InjectMocks
	private HomeController controller;

	private static final Integer PARENT_USER_ID = 1;
	private static final Integer CHILD_USER_ID = 10;

	private CustomUserDetails childLogin;
	private CustomUserDetails parentLogin;
	private Model model;

	@BeforeEach
	void setUp() {
		UserEntity child = new UserEntity();
		child.setUserId(CHILD_USER_ID);
		child.setUserName("子供太郎");
		child.setParentUserId(PARENT_USER_ID);
		child.setAuthority(CustomUserDetails.AUTHORITY_CHILD);
		childLogin = new CustomUserDetails(child);

		UserEntity parent = new UserEntity();
		parent.setUserId(PARENT_USER_ID);
		parent.setUserName("親太郎");
		parent.setAuthority(CustomUserDetails.AUTHORITY_PARENT);
		parentLogin = new CustomUserDetails(parent);

		model = new ExtendedModelMap();
	}

	@Nested
	@DisplayName("showChildrenHome")
	class ShowChildrenHome {

		@Test
		@DisplayName("子供ホームに必要なモデル属性が一通り設定される")
		void showChildrenHome_setsExpectedAttributes() {
			when(characterService.getCharacter(CHILD_USER_ID)).thenReturn(new CharacterDto());
			when(incomeExpenseService.getRecords(CHILD_USER_ID)).thenReturn(List.of());
			when(questService.getApprovalList()).thenReturn(List.of());
			when(spendingService.getPendingLimits(CHILD_USER_ID)).thenReturn(List.of());
			when(questTemplateService.findByParentUserId(CHILD_USER_ID)).thenReturn(List.of());
			when(spendingService.getCurrentLimit(CHILD_USER_ID)).thenReturn(null);
			when(spendingService.getChildLimitHistory(CHILD_USER_ID)).thenReturn(List.of());
			when(badgeService.getBadges(CHILD_USER_ID)).thenReturn(List.of());

			String view = controller.showChildrenHome(childLogin, model);

			assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.CHILD_HOME_HTML);
			assertThat(model.getAttribute("activeTab")).isEqualTo("home");
			assertThat(model.getAttribute("userName")).isEqualTo("子供太郎");
			assertThat(model.getAttribute("currentMoney")).isEqualTo(0);
			assertThat(model.getAttribute("characterForm")).isInstanceOf(CharacterForm.class);
		}

		@Test
		@DisplayName("収入・支出レコードから現在の所持金が正しく計算される")
		void showChildrenHome_calculatesCurrentMoney() {
			IncomeExpenseDto income = new IncomeExpenseDto();
			income.setRecordType(0);
			income.setAmount(1000);
			IncomeExpenseDto expense = new IncomeExpenseDto();
			expense.setRecordType(1);
			expense.setAmount(300);

			when(characterService.getCharacter(CHILD_USER_ID)).thenReturn(new CharacterDto());
			when(incomeExpenseService.getRecords(CHILD_USER_ID)).thenReturn(List.of(income, expense));
			when(questService.getApprovalList()).thenReturn(List.of());
			when(spendingService.getPendingLimits(CHILD_USER_ID)).thenReturn(List.of());
			when(questTemplateService.findByParentUserId(CHILD_USER_ID)).thenReturn(List.of());
			when(spendingService.getCurrentLimit(CHILD_USER_ID)).thenReturn(null);
			when(spendingService.getChildLimitHistory(CHILD_USER_ID)).thenReturn(List.of());
			when(badgeService.getBadges(CHILD_USER_ID)).thenReturn(List.of());

			controller.showChildrenHome(childLogin, model);

			assertThat(model.getAttribute("currentMoney")).isEqualTo(700);
		}
	}

	@Test
	@DisplayName("updateCharacterNameは更新後、子供ホームへリダイレクトする")
	void updateCharacterName_redirectsToChildHome() {
		CharacterForm form = new CharacterForm();
		form.setCharacterName("新しい名前");

		String result = controller.updateCharacterName(childLogin, form);

		assertThat(result).isEqualTo("redirect:" + TransitionTargetPageNameKeyword.CHILD_HOME);
	}

	@Nested
	@DisplayName("showParentHome")
	class ShowParentHome {

		@Test
		@DisplayName("子供がいない場合でも例外を投げずに空の集計を返す")
		void showParentHome_noChildren() {
			when(questTemplateService.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of());
			when(userService.getChildrenByParentId(PARENT_USER_ID)).thenReturn(List.of());
			when(questService.getApprovalList()).thenReturn(List.of());
			when(spendingService.getPendingLimits(PARENT_USER_ID)).thenReturn(List.of());

			String view = controller.showParentHome(parentLogin, model);

			assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.PARENT_HOME_HTML);
			assertThat(model.getAttribute("activeTab")).isEqualTo("overview");
			assertThat(model.getAttribute("questApprovalCount")).isEqualTo(0);
			assertThat(model.getAttribute("totalGiven")).isEqualTo(0);
			assertThat(model.getAttribute("totalSpent")).isEqualTo(0);
		}

		@Test
		@DisplayName("承認待ちクエストは自分の子供の分だけカウントされる")
		void showParentHome_countsOnlyOwnChildrenApprovals() {
			UserDto ownChild = new UserDto();
			ownChild.setUserId(CHILD_USER_ID);
			ownChild.setAuthority(CustomUserDetails.AUTHORITY_CHILD);

			UserEntity ownChildEntity = new UserEntity();
			ownChildEntity.setUserId(CHILD_USER_ID);
			ownChildEntity.setParentUserId(PARENT_USER_ID);

			UserEntity strangerChildEntity = new UserEntity();
			strangerChildEntity.setUserId(999);
			strangerChildEntity.setParentUserId(555);

			QuestEntity ownQuest = new QuestEntity();
			ownQuest.setChildUser(ownChildEntity);
			QuestEntity strangerQuest = new QuestEntity();
			strangerQuest.setChildUser(strangerChildEntity);

			when(questTemplateService.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of());
			when(userService.getChildrenByParentId(PARENT_USER_ID)).thenReturn(List.of(ownChild));
			when(questService.getApprovalList()).thenReturn(List.of(ownQuest, strangerQuest));
			when(spendingService.getPendingLimits(PARENT_USER_ID)).thenReturn(List.of());
			when(incomeExpenseService.getIncomeRecords(CHILD_USER_ID)).thenReturn(List.of());
			when(incomeExpenseService.getExpenseRecords(CHILD_USER_ID)).thenReturn(List.of());
			when(spendingService.getCurrentLimit(CHILD_USER_ID)).thenReturn(null);

			controller.showParentHome(parentLogin, model);

			assertThat(model.getAttribute("questApprovalCount")).isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("showParentBalance")
	class ShowParentBalance {

		@Test
		@DisplayName("childUserId未指定の場合は子供一覧の先頭が選択される")
		void showParentBalance_defaultsToFirstChild() {
			UserDto ownChild = new UserDto();
			ownChild.setUserId(CHILD_USER_ID);
			ownChild.setAuthority(CustomUserDetails.AUTHORITY_CHILD);

			when(questTemplateService.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of());
			when(userService.getChildrenByParentId(PARENT_USER_ID)).thenReturn(List.of(ownChild));
			when(questService.getApprovalList()).thenReturn(List.of());
			when(spendingService.getPendingLimits(PARENT_USER_ID)).thenReturn(List.of());
			when(incomeExpenseService.getIncomeRecords(CHILD_USER_ID)).thenReturn(List.of());
			when(incomeExpenseService.getExpenseRecords(CHILD_USER_ID)).thenReturn(List.of());

			String view = controller.showParentBalance(parentLogin, null, model);

			assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.PARENT_HOME_HTML);
			assertThat(model.getAttribute("selectedChildId")).isEqualTo(CHILD_USER_ID);
		}

		@Test
		@DisplayName("子供が1人もいない場合はselectedChildIdがnullになり収支レコードは取得しない")
		void showParentBalance_noChildren_selectedChildIdNull() {
			when(questTemplateService.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of());
			when(userService.getChildrenByParentId(PARENT_USER_ID)).thenReturn(List.of());
			when(questService.getApprovalList()).thenReturn(List.of());
			when(spendingService.getPendingLimits(PARENT_USER_ID)).thenReturn(List.of());

			controller.showParentBalance(parentLogin, null, model);

			assertThat(model.getAttribute("selectedChildId")).isNull();
			assertThat(model.containsAttribute("incomeRecords")).isFalse();
		}
	}
}
