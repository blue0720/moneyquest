package com.example.moneyquest.app.presentation.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.moneyquest.app.domain.model.BattleResultDto;
import com.example.moneyquest.app.domain.service.BadgeService;
import com.example.moneyquest.app.domain.service.BattleService;
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
import com.example.moneyquest.app.presentation.form.IncomeExpenseForm;
import com.example.moneyquest.app.presentation.form.SpendingLimitForm;

/**
 * 👦 子供用機能: NPC対戦タブ(将来機能3)。
 */
@Controller
public class BattleController {

	private final BattleService battleService;
	private final CharacterService characterService;
	private final BadgeService badgeService;
	private final IncomeExpenseService incomeExpenseService;
	private final SpendingService spendingService;
	private final UserService userService;
	private final QuestService questService;
	private final QuestTemplateService questTemplateService;

	public BattleController(
			BattleService battleService,
			CharacterService characterService,
			BadgeService badgeService,
			IncomeExpenseService incomeExpenseService,
			SpendingService spendingService,
			UserService userService,
			QuestService questService,
			QuestTemplateService questTemplateService) {
		this.battleService = battleService;
		this.characterService = characterService;
		this.badgeService = badgeService;
		this.incomeExpenseService = incomeExpenseService;
		this.spendingService = spendingService;
		this.userService = userService;
		this.questService = questService;
		this.questTemplateService = questTemplateService;
	}

	/**
	 * たいせんタブ表示 (GET /child/battle)
	 */
	@GetMapping(TransitionTargetPageNameKeyword.CHILD_BATTLE)
	public String showBattleTab(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			Model model) {

		Integer childUserId = loginUser.getUserId();

		Integer currentMoney = incomeExpenseService.getRecords(childUserId).stream()
				.mapToInt(record -> {
					Integer amount = record.getAmount() == null ? 0 : record.getAmount();
					return record.getRecordType() == 0 ? amount : -amount;
				})
				.sum();

		IncomeExpenseForm incomeExpenseForm = new IncomeExpenseForm();
		incomeExpenseForm.setRecordType(1);
		incomeExpenseForm.setChildUserId(childUserId);

		ParentModelHelper.setDefaults(model,
				loginUser,
				userService,
				questService, questTemplateService,
				spendingService);
		model.addAttribute("userName", loginUser.getUser().getUserName());
		model.addAttribute("currentMoney", currentMoney);
		model.addAttribute("activeTab", "battle");

		model.addAttribute("character", characterService.getCharacter(childUserId));
		model.addAttribute("characterForm", new CharacterForm());
		model.addAttribute("badgeList", badgeService.getBadges(childUserId));
		model.addAttribute("npcList", battleService.getNpcList(childUserId));

		model.addAttribute("currentLimit", spendingService.getCurrentLimit(childUserId));
		model.addAttribute("limitHistory", spendingService.getChildLimitHistory(childUserId));
		model.addAttribute("spendingLimitForm", new SpendingLimitForm());

		model.addAttribute("incomeExpenseForm", incomeExpenseForm);

		return TransitionTargetPageNameKeyword.CHILD_HOME_HTML;
	}

	/**
	 * NPCへの挑戦 (POST /child/battle/{code}/challenge)
	 */
	@PostMapping(TransitionTargetPageNameKeyword.CHILD_BATTLE_CHALLENGE)
	public String challenge(
			@PathVariable("code") String npcCode,
			@AuthenticationPrincipal CustomUserDetails loginUser,
			RedirectAttributes redirectAttributes) {

		BattleResultDto result = battleService.challenge(loginUser.getUserId(), npcCode);
		redirectAttributes.addFlashAttribute("battleResult", result);

		return "redirect:" + TransitionTargetPageNameKeyword.CHILD_BATTLE;
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public String handleIllegalArgumentException(IllegalArgumentException e) {
		return TransitionTargetPageNameKeyword.ERROR_HTML;
	}

}
