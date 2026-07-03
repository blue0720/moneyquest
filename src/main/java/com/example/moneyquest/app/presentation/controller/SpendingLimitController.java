package com.example.moneyquest.app.presentation.controller;

import static com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword.*;

import java.beans.PropertyEditorSupport;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.moneyquest.app.domain.service.CharacterService;
import com.example.moneyquest.app.domain.service.CustomUserDetails;
import com.example.moneyquest.app.domain.service.IncomeExpenseService;
import com.example.moneyquest.app.domain.service.QuestService;
import com.example.moneyquest.app.domain.service.QuestTemplateService;
import com.example.moneyquest.app.domain.service.SpendingService;
import com.example.moneyquest.app.domain.service.UserService;
import com.example.moneyquest.app.presentation.common.ParentModelHelper;
import com.example.moneyquest.app.presentation.form.IncomeExpenseForm;
import com.example.moneyquest.app.presentation.form.SpendingLimitForm;

@Controller
public class SpendingLimitController {

	private final SpendingService spendingService;
	private final CharacterService characterService;
	private final IncomeExpenseService incomeExpenseService;
	private final UserService userService;
	private final QuestService questService;
	private final QuestTemplateService questTemplateService;

	public SpendingLimitController(SpendingService spendingService, CharacterService characterService,
			IncomeExpenseService incomeExpenseService, UserService userService,
			QuestService questService,
			QuestTemplateService questTemplateService) {
		this.spendingService = spendingService;
		this.characterService = characterService;
		this.incomeExpenseService = incomeExpenseService;
		this.userService = userService;
		this.questService = questService;
		this.questTemplateService = questTemplateService;
	}

	/** 子供：上限申請タブ */
	@GetMapping(CHILD_LIMIT)
	public String showSpendingLimits(
			@AuthenticationPrincipal CustomUserDetails loginUser, Model model) {

		Integer childUserId = loginUser.getUserId();

		model.addAttribute("userName", loginUser.getUser().getUserName());
		model.addAttribute("activeTab", "limit");
		model.addAttribute("character", characterService.getCharacter(childUserId));
		model.addAttribute("currentLimit", spendingService.getCurrentLimit(childUserId));
		model.addAttribute("historyList", spendingService.getChildLimitHistory(childUserId));
		model.addAttribute("spendingLimitForm", new SpendingLimitForm());
		model.addAttribute("currentMoney",
				incomeExpenseService.getRecords(childUserId).stream()
						.mapToInt(r -> r.getRecordType() == 0
								? (r.getAmount() == null ? 0 : r.getAmount())
								: -(r.getAmount() == null ? 0 : r.getAmount()))
						.sum());

		model.addAttribute("pendingLimit", spendingService.getCurrentLimit(childUserId));

		IncomeExpenseForm incomeExpenseForm = new IncomeExpenseForm();
		incomeExpenseForm.setRecordType(1);
		incomeExpenseForm.setChildUserId(childUserId);
		model.addAttribute("incomeExpenseForm", incomeExpenseForm);

		return CHILD_HOME_HTML;
	}

	/**
	 * 子供：支出上限申請
	 */
	@PostMapping(CHILD_LIMIT)
	public String requestLimit(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			SpendingLimitForm form) {

		spendingService.requestLimit(
				form,
				loginUser.getUserId());

		return "redirect:" + CHILD_LIMIT;
	}

	/**
	 * 保護者：承認待ち,過去の達成状況一覧表示
	 */
	@GetMapping(PARENT_LIMIT)
	public String showPendingLimits(
			@AuthenticationPrincipal CustomUserDetails loginUser, Model model) {

		Integer parentUserId = loginUser.getUserId(); //ログインユーザー取得(親

		ParentModelHelper.setDefaults(model,
				loginUser,
				userService,
				questService, questTemplateService,
				spendingService);
		model.addAttribute("userName", loginUser.getUser().getUserName());
		model.addAttribute("pendingList", spendingService.getPendingLimits(parentUserId));
		model.addAttribute("historyList", spendingService.getParentLimitHistory(parentUserId));

		model.addAttribute("activeTab", "limit");

		return "parent-home";
	}

	/**
	 * 保護者：承認
	 */
	@PostMapping(PARENT_LIMIT_APPROVE)
	public String approveLimit(
			@PathVariable("id") Integer id, @AuthenticationPrincipal CustomUserDetails loginUser) {

		spendingService.approveLimit(id, loginUser.getUser().getUserId());

		return "redirect:" + PARENT_LIMIT;
	}

	/**
	 * 保護者：却下
	 */
	@PostMapping(PARENT_LIMIT_REJECT)
	public String rejectLimit(
			@PathVariable("id") Integer id) {

		spendingService.rejectLimit(id);

		return "redirect:" + PARENT_LIMIT;
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(Integer.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) {
				if (text == null || text.trim().isEmpty()) {
					setValue(null);
				} else {
					try {
						setValue(Integer.parseInt(text.trim()));
					} catch (NumberFormatException e) {
						setValue(null);
					}
				}
			}
		});
	}

}
