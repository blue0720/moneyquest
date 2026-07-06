package com.example.moneyquest.app.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.example.moneyquest.app.domain.model.SavingDto;
import com.example.moneyquest.app.domain.service.CharacterService;
import com.example.moneyquest.app.domain.service.CustomUserDetails;
import com.example.moneyquest.app.domain.service.GraphService;
import com.example.moneyquest.app.domain.service.IncomeExpenseService;
import com.example.moneyquest.app.domain.service.QuestService;
import com.example.moneyquest.app.domain.service.QuestTemplateService;
import com.example.moneyquest.app.domain.service.SpendingService;
import com.example.moneyquest.app.domain.service.UserService;
import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword;

/**
 * GraphController の単体テスト。
 * 子供・保護者それぞれのグラフ画面表示と、グラフデータAPIの応答内容を確認する。
 */
@ExtendWith(MockitoExtension.class)
class GraphControllerTest {

	@Mock
	private GraphService graphService;
	@Mock
	private IncomeExpenseService incomeExpenseService;
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

	@InjectMocks
	private GraphController controller;

	private static final Integer CHILD_USER_ID = 10;
	private static final Integer PARENT_USER_ID = 1;

	private CustomUserDetails childLogin;
	private Model model;

	@BeforeEach
	void setUp() {
		UserEntity child = new UserEntity();
		child.setUserId(CHILD_USER_ID);
		child.setUserName("子供太郎");
		childLogin = new CustomUserDetails(child);
		model = new ExtendedModelMap();
	}

	@Test
	@DisplayName("子供のグラフ画面ではグラフデータ・所持金・上限情報が一通り設定される")
	void showChildGraph_setsExpectedAttributes() {
		when(graphService.getGraphData(CHILD_USER_ID)).thenReturn(new SavingDto());
		when(graphService.getMonthlyIncome(CHILD_USER_ID)).thenReturn(List.of());
		when(graphService.getMonthlyExpense(CHILD_USER_ID)).thenReturn(List.of());
		when(incomeExpenseService.getIncomeRecords(CHILD_USER_ID)).thenReturn(List.of());
		when(incomeExpenseService.getExpenseRecords(CHILD_USER_ID)).thenReturn(List.of());
		when(questService.getApprovalList()).thenReturn(List.of());
		when(spendingService.getPendingLimits(CHILD_USER_ID)).thenReturn(List.of());
		when(questTemplateService.findByParentUserId(CHILD_USER_ID)).thenReturn(List.of());
		when(characterService.getCharacter(CHILD_USER_ID)).thenReturn(null);
		when(spendingService.getCurrentLimit(CHILD_USER_ID)).thenReturn(null);
		when(incomeExpenseService.getRecords(CHILD_USER_ID)).thenReturn(List.of());

		String view = controller.showChildGraph(childLogin, model, childLogin);

		assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.CHILD_HOME_HTML);
		assertThat(model.getAttribute("activeTab")).isEqualTo("graph");
		assertThat(model.getAttribute("userName")).isEqualTo("子供太郎");
		assertThat(model.getAttribute("currentMoney")).isEqualTo(0);
	}

	@Test
	@DisplayName("保護者のグラフ画面は指定したchildUserIdのデータで表示される")
	void showParentGraph_usesRequestedChildId() {
		UserEntity parent = new UserEntity();
		parent.setUserId(PARENT_USER_ID);
		parent.setUserName("親太郎");
		CustomUserDetails parentLogin = new CustomUserDetails(parent);

		when(graphService.getGraphData(CHILD_USER_ID)).thenReturn(new SavingDto());
		when(graphService.getMonthlyIncome(CHILD_USER_ID)).thenReturn(List.of());
		when(graphService.getMonthlyExpense(CHILD_USER_ID)).thenReturn(List.of());
		when(incomeExpenseService.getIncomeRecords(CHILD_USER_ID)).thenReturn(List.of());
		when(incomeExpenseService.getExpenseRecords(CHILD_USER_ID)).thenReturn(List.of());
		when(questService.getApprovalList()).thenReturn(List.of());
		when(spendingService.getPendingLimits(PARENT_USER_ID)).thenReturn(List.of());
		when(questTemplateService.findByParentUserId(PARENT_USER_ID)).thenReturn(List.of());

		String view = controller.showParentGraph(parentLogin, CHILD_USER_ID, model, parentLogin);

		assertThat(view).isEqualTo(TransitionTargetPageNameKeyword.PARENT_HOME_HTML);
		assertThat(model.getAttribute("graphData")).isNotNull();
	}

	@Test
	@DisplayName("グラフデータAPIは集計値をMapに詰めて返す")
	void getParentGraphData_returnsExpectedMap() {
		SavingDto graphData = new SavingDto();
		graphData.setLavels(List.of("2026/01"));
		graphData.setCumulativeBalance(List.of(100));
		graphData.setMonthlyBalance(List.of(100));
		graphData.setTotalIncome(500);
		graphData.setTotalExpense(400);
		graphData.setSaving(100);

		when(graphService.getGraphData(CHILD_USER_ID)).thenReturn(graphData);
		when(graphService.getMonthlyIncome(CHILD_USER_ID)).thenReturn(List.of(500));
		when(graphService.getMonthlyExpense(CHILD_USER_ID)).thenReturn(List.of(400));

		Map<String, Object> result = controller.getParentGraphData(CHILD_USER_ID);

		assertThat(result.get("totalIncome")).isEqualTo(500);
		assertThat(result.get("totalExpense")).isEqualTo(400);
		assertThat(result.get("saving")).isEqualTo(100);
		assertThat(result.get("labels")).isEqualTo(List.of("2026/01"));
		assertThat(result.get("monthlyIncome")).isEqualTo(List.of(500));
		assertThat(result.get("monthlyExpense")).isEqualTo(List.of(400));
	}
}
