package com.example.moneyquest.app.presentation.controller;

import static com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword.*;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.moneyquest.app.domain.model.SavingDto;
import com.example.moneyquest.app.domain.service.CharacterService;
import com.example.moneyquest.app.domain.service.CustomUserDetails;
import com.example.moneyquest.app.domain.service.GraphService;
import com.example.moneyquest.app.domain.service.IncomeExpenseService;
import com.example.moneyquest.app.domain.service.QuestService;
import com.example.moneyquest.app.domain.service.QuestTemplateService;
import com.example.moneyquest.app.domain.service.SpendingService;
import com.example.moneyquest.app.domain.service.UserService;
import com.example.moneyquest.app.presentation.common.ParentModelHelper;
import com.example.moneyquest.app.presentation.form.IncomeExpenseForm;

@Controller
public class GraphController {

	private final GraphService graphService;
	private final IncomeExpenseService incomeExpenseService;
	private final CharacterService characterService;
	private final SpendingService spendingService;
	private final UserService userService;
	private final QuestService questService;
	private final QuestTemplateService questTemplateService;

	public GraphController(
			GraphService graphService,
			IncomeExpenseService incomeExpenseService,
			CharacterService characterService,
			SpendingService spendingService,
			UserService userService,
			QuestService questService,
			QuestTemplateService questTemplateService) {
		this.graphService = graphService;
		this.incomeExpenseService = incomeExpenseService;
		this.characterService = characterService;
		this.spendingService = spendingService;
		this.userService = userService;
		this.questService = questService;
		this.questTemplateService = questTemplateService;
	}

	@GetMapping(CHILD_GRAPH)
	public String showChildGraph(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			Model model, CustomUserDetails loginUser) {

		Integer childUserId = userDetails.getUserId();

		// グラフデータ
		setGraphModel(childUserId, model, userDetails);

		model.addAttribute("userName", userDetails.getUser().getUserName());
		model.addAttribute("activeTab", "graph");
		model.addAttribute("character", characterService.getCharacter(childUserId));
		model.addAttribute("currentLimit", spendingService.getCurrentLimit(childUserId));
		model.addAttribute("currentMoney",
				incomeExpenseService.getRecords(childUserId).stream()
						.mapToInt(r -> r.getRecordType() == 0
								? (r.getAmount() == null ? 0 : r.getAmount())
								: -(r.getAmount() == null ? 0 : r.getAmount()))
						.sum());

		IncomeExpenseForm incomeExpenseForm = new IncomeExpenseForm();
		incomeExpenseForm.setRecordType(1);
		incomeExpenseForm.setChildUserId(childUserId);
		model.addAttribute("incomeExpenseForm", incomeExpenseForm);

		return CHILD_HOME_HTML;
	}

	@GetMapping(PARENT_GRAPH)
	public String showParentGraph(@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestParam("childUserId") Integer childUserId, Model model, CustomUserDetails loginUser) {
		setGraphModel(childUserId, model, userDetails);
		return PARENT_HOME_HTML;
	}

	@GetMapping("/parent/graph/data")
	@ResponseBody
	public Map<String, Object> getParentGraphData(
			@RequestParam("childUserId") Integer childUserId) {

		SavingDto graphData = graphService.getGraphData(childUserId);

		Map<String, Object> result = new HashMap<>();

		result.put("labels", graphData.getLavels());
		result.put("cumulativeBalance", graphData.getCumulativeBalance());
		result.put("monthlyBalance", graphData.getMonthlyBalance());
		result.put("totalIncome", graphData.getTotalIncome());
		result.put("totalExpense", graphData.getTotalExpense());
		result.put("saving", graphData.getSaving());
		result.put("monthlyIncome", graphService.getMonthlyIncome(childUserId));
		result.put("monthlyExpense", graphService.getMonthlyExpense(childUserId));

		return result;

	}

	private void setGraphModel(Integer childUserId, Model model, CustomUserDetails loginUser) {
		SavingDto graphData = graphService.getGraphData(childUserId);
		ParentModelHelper.setDefaults(model, loginUser, userService, questService, questTemplateService,
				spendingService);
		model.addAttribute("graphData", graphData);
		model.addAttribute("monthlyIncome", graphService.getMonthlyIncome(childUserId));
		model.addAttribute("monthlyExpense", graphService.getMonthlyExpense(childUserId));
		model.addAttribute("incomeRecords", incomeExpenseService.getIncomeRecords(childUserId));
		model.addAttribute("expenseRecords", incomeExpenseService.getExpenseRecords(childUserId));
	}
}