package com.example.moneyquest.app.presentation.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;

import com.example.moneyquest.app.domain.model.IncomeExpenseDto;
import com.example.moneyquest.app.domain.service.BadgeService;
import com.example.moneyquest.app.domain.service.CharacterService;
import com.example.moneyquest.app.domain.service.CustomUserDetails;
import com.example.moneyquest.app.domain.service.IncomeExpenseService;
import com.example.moneyquest.app.domain.service.QuestService;
import com.example.moneyquest.app.domain.service.QuestTemplateService;
import com.example.moneyquest.app.domain.service.SpendingService;
import com.example.moneyquest.app.domain.service.UserService;
import com.example.moneyquest.app.presentation.common.ParentModelHelper;
import com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword;
import com.example.moneyquest.app.presentation.form.CharacterForm;
import com.example.moneyquest.app.presentation.form.CharacterTypeForm;
import com.example.moneyquest.app.presentation.form.IncomeExpenseForm;
import com.example.moneyquest.app.presentation.form.SpendingLimitForm;

@Controller
public class HomeController {

	private final CharacterService characterService;
	private final IncomeExpenseService incomeExpenseService;
	private final QuestService questService;
	private final SpendingService spendingService;
	private final QuestTemplateService questTemplateService;
	private final UserService userService;
	private final BadgeService badgeService;

	public HomeController(
			CharacterService characterService,
			IncomeExpenseService incomeExpenseService,
			QuestService questService,
			SpendingService spendingService,
			QuestTemplateService questTemplateService,
			UserService userService,
			BadgeService badgeService) {
		this.characterService = characterService;
		this.incomeExpenseService = incomeExpenseService;
		this.questService = questService;
		this.spendingService = spendingService;
		this.questTemplateService = questTemplateService;
		this.userService = userService;
		this.badgeService = badgeService;
	}

	@GetMapping(TransitionTargetPageNameKeyword.CHILD_HOME)
	public String showChildrenHome(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			Model model) {

		Integer childUserId = loginUser.getUserId();

		var character = characterService.getCharacter(childUserId);
		Integer currentMoney = calcCurrentMoney(childUserId);

		ParentModelHelper.setDefaults(model, loginUser, userService, questService, questTemplateService,
				spendingService);
		model.addAttribute("userName", loginUser.getUser().getUserName());
		model.addAttribute("currentMoney", currentMoney);
		model.addAttribute("character", character);
		model.addAttribute("characterForm", new CharacterForm());
		model.addAttribute("badgeList", badgeService.getBadges(childUserId));
		model.addAttribute("activeTab", "home");

		model.addAttribute("currentLimit", spendingService.getCurrentLimit(childUserId));
		model.addAttribute("limitHistory", spendingService.getChildLimitHistory(childUserId));
		model.addAttribute("spendingLimitForm", new SpendingLimitForm());

		IncomeExpenseForm incomeExpenseForm = new IncomeExpenseForm();
		incomeExpenseForm.setRecordType(1);
		incomeExpenseForm.setChildUserId(childUserId);
		model.addAttribute("incomeExpenseForm", incomeExpenseForm);

		return TransitionTargetPageNameKeyword.CHILD_HOME_HTML;
	}

	@PostMapping(TransitionTargetPageNameKeyword.CHILD_CHARACTER_NAME)
	public String updateCharacterName(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			CharacterForm form) {

		Integer childUserId = loginUser.getUserId();

		characterService.updateCharacterName(
				childUserId,
				form.getCharacterName());

		return "redirect:" + TransitionTargetPageNameKeyword.CHILD_HOME;
	}

	@PostMapping(TransitionTargetPageNameKeyword.CHILD_CHARACTER_TYPE)
	public String updateCharacterType(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			@Valid CharacterTypeForm form,
			BindingResult bindingResult) {

		if (bindingResult.hasErrors()) {
			return "redirect:" + TransitionTargetPageNameKeyword.CHILD_HOME;
		}

		Integer childUserId = loginUser.getUserId();

		characterService.updateCharacterType(
				childUserId,
				form.getCharacterType());

		return "redirect:" + TransitionTargetPageNameKeyword.CHILD_HOME;
	}

	@GetMapping(TransitionTargetPageNameKeyword.PARENT_HOME)
	public String showParentHome(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			Model model) {

		final Integer loginParentUserId = loginUser.getUserId();

		var templateList = questTemplateService.findByParentUserId(loginParentUserId);
		var childList = userService.getChildrenByParentId(loginParentUserId);

		Integer questApprovalCount = questService.getApprovalList().stream()
				.filter(quest -> quest.getChildUser() != null)
				.filter(quest -> loginParentUserId.equals(quest.getChildUser().getParentUserId()))
				.toList()
				.size();

		Integer limitApprovalCount = spendingService.getPendingLimits(loginParentUserId).size();

		// 子供ごとの貯金額
		Map<Integer, Integer> currentMoneyMap = new HashMap<>();
		for (var child : childList) {
			currentMoneyMap.put(child.getUserId(), calcCurrentMoney(child.getUserId()));
		}

		// 今月のサマリー（支給合計・使った合計）
		int totalIncome = currentMoneyMap.values().stream().mapToInt(Integer::intValue).sum();
		// 保護者が支給した合計（recordType=0の全子供分）
		int totalGiven = childList.stream()
				.flatMap(child -> incomeExpenseService.getIncomeRecords(child.getUserId()).stream())
				.mapToInt(r -> r.getAmount() == null ? 0 : r.getAmount())
				.sum();
		// 子供が使った合計（recordType=1の全子供分）
		int totalSpent = childList.stream()
				.flatMap(child -> incomeExpenseService.getExpenseRecords(child.getUserId()).stream())
				.mapToInt(r -> r.getAmount() == null ? 0 : r.getAmount())
				.sum();

		// 子供ごとの上限状況
		Map<Integer, Object> limitMap = new HashMap<>();
		for (var child : childList) {
			limitMap.put(child.getUserId(), spendingService.getCurrentLimit(child.getUserId()));
		}

		ParentModelHelper.setDefaults(model, loginUser, userService, questService, questTemplateService,
				spendingService);
		model.addAttribute("userName", loginUser.getUser().getUserName());
		model.addAttribute("activeTab", "overview");

		model.addAttribute("questApprovalCount", questApprovalCount);
		model.addAttribute("limitApprovalCount", limitApprovalCount);
		model.addAttribute("templateCount", templateList.size());

		model.addAttribute("templateList", templateList);
		model.addAttribute("childList", childList);

		model.addAttribute("currentMoneyMap", currentMoneyMap);
		model.addAttribute("totalGiven", totalGiven);
		model.addAttribute("totalSpent", totalSpent);
		model.addAttribute("limitMap", limitMap);

		return TransitionTargetPageNameKeyword.PARENT_HOME_HTML;
	}

	@GetMapping(TransitionTargetPageNameKeyword.PARENT_BALANCE)
	public String showParentBalance(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			@RequestParam(value = "childUserId", required = false) Integer childUserId,
			Model model) {

		final Integer loginParentUserId = loginUser.getUserId();

		var templateList = questTemplateService.findByParentUserId(loginParentUserId);
		var childList = userService.getChildrenByParentId(loginParentUserId);

		Integer questApprovalCount = questService.getApprovalList().stream()
				.filter(quest -> quest.getChildUser() != null)
				.filter(quest -> loginParentUserId.equals(quest.getChildUser().getParentUserId()))
				.toList().size();

		Integer limitApprovalCount = spendingService.getPendingLimits(loginParentUserId).size();

		Integer selectedChildId = childUserId != null ? childUserId
				: (childList.isEmpty() ? null : childList.get(0).getUserId());

		ParentModelHelper.setDefaults(model, loginUser, userService, questService, questTemplateService,
				spendingService);
		model.addAttribute("activeTab", "balance");
		model.addAttribute("questApprovalCount", questApprovalCount);
		model.addAttribute("limitApprovalCount", limitApprovalCount);
		model.addAttribute("templateCount", templateList.size());
		model.addAttribute("templateList", templateList);
		model.addAttribute("childList", childList);
		model.addAttribute("selectedChildId", selectedChildId);

		if (selectedChildId != null) {
			model.addAttribute("incomeRecords",
					incomeExpenseService.getIncomeRecords(selectedChildId));
			model.addAttribute("expenseRecords",
					incomeExpenseService.getExpenseRecords(selectedChildId));
		}

		return TransitionTargetPageNameKeyword.PARENT_HOME_HTML;
	}

	private Integer calcCurrentMoney(Integer childUserId) {
		List<IncomeExpenseDto> records = incomeExpenseService.getRecords(childUserId);

		int incomeTotal = records.stream()
				.filter(record -> record.getRecordType() == 0)
				.mapToInt(record -> record.getAmount() == null ? 0 : record.getAmount())
				.sum();

		int expenseTotal = records.stream()
				.filter(record -> record.getRecordType() == 1)
				.mapToInt(record -> record.getAmount() == null ? 0 : record.getAmount())
				.sum();

		return incomeTotal - expenseTotal;
	}
}