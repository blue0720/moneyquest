package com.example.moneyquest.app.presentation.controller;

import java.beans.PropertyEditorSupport;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

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
import com.example.moneyquest.app.presentation.common.ParentModelHelper;
import com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword;
import com.example.moneyquest.app.presentation.form.CharacterForm;
import com.example.moneyquest.app.presentation.form.IncomeExpenseForm;
import com.example.moneyquest.app.presentation.form.QuestSendForm;
import com.example.moneyquest.app.presentation.form.QuestTemplateForm;
import com.example.moneyquest.app.presentation.form.SpendingLimitForm;

@Controller
public class QuestController {
	private final QuestService questService;
	private final UserService userService;
	private final SpendingService spendingService;
	private final CharacterService characterService;
	private final IncomeExpenseService incomeExpenseService;
	private final QuestTemplateService questTemplateService;
	private final BadgeService badgeService;

	public QuestController(
			QuestService questService,
			UserService userService,
			SpendingService spendingService,
			CharacterService characterService,
			IncomeExpenseService incomeExpenseService,
			QuestTemplateService questTemplateService,
			BadgeService badgeService) {

		this.questService = questService;
		this.userService = userService;
		this.spendingService = spendingService;
		this.characterService = characterService;
		this.incomeExpenseService = incomeExpenseService;
		this.questTemplateService = questTemplateService;
		this.badgeService = badgeService;
	}

	@ModelAttribute
	public QuestSendForm questSendForm() {
		return new QuestSendForm();
	}

	// ==========================================
	// 👨‍👩‍👧 保護者用機能
	// ==========================================

	/**
	 * 1. 登録済みクエスト一覧 (GET /parent/quest)
	 */
	@GetMapping(TransitionTargetPageNameKeyword.PARENT_QUESTS)
	public String showQuests(@AuthenticationPrincipal CustomUserDetails loginUser, QuestSendForm questSendForm,
			BindingResult bindingResult, Model model) {

		Integer parentUserId = loginUser.getUserId();

		// 保護者に紐づく子供一覧を取得
		var childList = userService.getChildrenByParentId(parentUserId);

		// 子供全員のクエストを取得
		List<Integer> targetStatuses=List.of(0,1,4,5);
		List<QuestEntity> questList = childList.stream()
				.flatMap(child -> questService.getQuestsByChildAndStatuses(child.getUserId(),targetStatuses).stream())
				.toList();

		ParentModelHelper.setDefaults(model, loginUser, userService, questService, questTemplateService,
				spendingService);
		model.addAttribute("questList", questList);
		model.addAttribute("activeTab", "quest");
		model.addAttribute("childList", childList);

		return TransitionTargetPageNameKeyword.PARENT_HOME_HTML;
	}

	/**
	 * 2. クエスト作成 (POST /parent/quest)
	 */
	@PostMapping(TransitionTargetPageNameKeyword.PARENT_QUESTS)
	public String createQuest(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			@Validated QuestSendForm questSendForm,
			BindingResult bindingResult,
			Model model) {

		Integer parentUserId = loginUser.getUserId();

		var childList = userService.getChildrenByParentId(parentUserId);

		if (bindingResult.hasErrors()) {
			List<QuestEntity> questList = childList.stream()
					.flatMap(child -> questService.getQuestsByChild(child.getUserId()).stream())
					.toList();

			ParentModelHelper.setDefaults(model,
					loginUser,
					userService,
					questService, questTemplateService,
					spendingService);
			model.addAttribute("questList", questList);
			model.addAttribute("childList", childList);
			model.addAttribute("activeTab", "quest");

			return TransitionTargetPageNameKeyword.PARENT_HOME_HTML;
		}

		if (questSendForm.getChildUserId() != null && questSendForm.getChildUserId() == 0) {
			for (UserDto child : childList) {
				QuestSendForm form = new QuestSendForm();
				form.setChildUserId(child.getUserId());
				form.setTitle(questSendForm.getTitle());
				form.setRewardAmount(questSendForm.getRewardAmount());
				form.setExp(questSendForm.getExp());
				form.setCoinReward(questSendForm.getCoinReward());
				form.setDescription(questSendForm.getDescription());
				form.setLimitAmount(questSendForm.getLimitAmount());
				form.setAvailableDays(questSendForm.getAvailableDays());
				form.setSpecificDate(questSendForm.getSpecificDate());

				questService.createQuest(form,loginUser.getUser().getUserId());
			}
		} else {
			questService.createQuest(questSendForm,loginUser.getUser().getUserId());
		}

		if (Boolean.TRUE.equals(questSendForm.getSaveAsTemplate())) {

			QuestTemplateForm templateForm = new QuestTemplateForm();

			templateForm.setTitle(questSendForm.getTitle());
			templateForm.setRewardAmount(questSendForm.getRewardAmount());
			templateForm.setExp(
					questSendForm.getExp() == null
							? 5
							: questSendForm.getExp());

			templateForm.setDescription(
					questSendForm.getDescription());

			questTemplateService.addTemplate(
					parentUserId,
					templateForm);
		}

		return "redirect:" + TransitionTargetPageNameKeyword.PARENT_QUESTS;
	}

	/**
	 * 3. クエスト編集 (POST /parent/quest/{id}/edit)
	 */

	@PostMapping(TransitionTargetPageNameKeyword.PARENT_QUESTS_EDIT)
	public String updateQuest(
			@PathVariable("id") Integer questId,
			@AuthenticationPrincipal CustomUserDetails loginUser,
			@Validated QuestSendForm questSendForm,
			BindingResult bindingResult,
			Model model) {

		questSendForm.setQuestId(questId);

		if (bindingResult.hasErrors()) {
			Integer childUserId = loginUser.getUserId();
			ParentModelHelper.setDefaults(model,
					loginUser,
					userService,
					questService, questTemplateService,
					spendingService);
			model.addAttribute("questList", questService.getQuestsByChild(childUserId));
			return "redirect:"+TransitionTargetPageNameKeyword.PARENT_QUESTS;
		}

		questService.updateQuest(questSendForm, loginUser.getUserId());
		return "redirect:" + TransitionTargetPageNameKeyword.PARENT_QUESTS;
	}

	/**
	 * 4. クエスト削除 (POST /parent/quest/{id}/delete)
	 */

	@PostMapping(TransitionTargetPageNameKeyword.PARENT_QUESTS_DELETE)
	public String deleteQuest(
			@PathVariable("id") Integer questId,
			@AuthenticationPrincipal CustomUserDetails loginUser) {
		questService.deleteQuest(questId, loginUser.getUserId());
		return "redirect:" + TransitionTargetPageNameKeyword.PARENT_QUESTS;

	}

	/**
	 * 5. 承認クエスト一覧表示 (GET /parent/approvals)
	 */

	@GetMapping(TransitionTargetPageNameKeyword.PARENT_APPROVALS)
	public String showAppovalList(@AuthenticationPrincipal CustomUserDetails loginUser, QuestSendForm questSendForm,
			Model model) {
		Integer parentUserId = loginUser.getUserId();
		List<Integer> approvalStatus = List.of(1);

		// 保護者に紐づく全子供を取得
		var childList = userService.getChildrenByParentId(parentUserId);

		// 全子供のクエスト申請を取得
		List<QuestEntity> approvalList = childList.stream()
				.flatMap(child -> questService
						.getQuestsByChildAndStatuses(child.getUserId(), approvalStatus).stream())
				.toList();
		//		System.out.println("approvalList size: " + approvalList.size()); // ← 追加
		// 画面へ渡す
		ParentModelHelper.setDefaults(model,
				loginUser,
				userService,
				questService, questTemplateService,
				spendingService);
		model.addAttribute("approvalList", approvalList);
		model.addAttribute("activeTab", "approvals");

		return TransitionTargetPageNameKeyword.PARENT_HOME_HTML;
	}

	/**
	 * 6. クエスト承認 (POST /parent/approvals/{id}/approve)
	 */

	@PostMapping(TransitionTargetPageNameKeyword.PARENT_APPROVALS_APPROVE)
	public String approveQuest(@AuthenticationPrincipal CustomUserDetails loginUser,
			@PathVariable("id") Integer questId,
			QuestSendForm questSendForm) {

		questService.approveQuest(questSendForm, questId, loginUser.getUser());

		return "redirect:" + TransitionTargetPageNameKeyword.PARENT_APPROVALS;
	}

	/**
	 * 7. クエスト 却下 (POST /parent/quest-approvals/{id}/reject)
	 */
	@PostMapping(TransitionTargetPageNameKeyword.PARENT_APPROVALS_REJECT)
	public String rejectQuest(
			@PathVariable("id") Integer questId,
			@AuthenticationPrincipal CustomUserDetails loginUser) {

		// サービス側の「7. rejectQuest」を呼び出して却下（ステータス3化）を実行
		questService.rejectQuest(questId, loginUser.getUserId());

		// 🔄 却下完了後は、承認待ち「一覧画面」にリダイレクトして戻る
		return "redirect:" + TransitionTargetPageNameKeyword.PARENT_APPROVALS;
	}

	// ==========================================
	// 👦 子供用機能 (SCR-05-2)
	// ==========================================

	/**
	 * 8. 受信クエスト一覧 (GET /child/quest)
	 */
	@GetMapping(TransitionTargetPageNameKeyword.CHILD_QUESTS)
	public String showChildQuests(
			QuestSendForm questSendForm,
			@AuthenticationPrincipal CustomUserDetails loginUser,
			BindingResult bindingResult,
			Model model) {

		Integer childUserId = loginUser.getUserId();

		List<Integer> activeStatuses = List.of(0, 1, 4, 5);
		List<QuestEntity> questList = questService.getQuestsByChildAndStatuses(childUserId, activeStatuses);

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
		model.addAttribute("questList", questList);
		model.addAttribute("activeTab", "quest");

		model.addAttribute("character", characterService.getCharacter(childUserId));
		model.addAttribute("characterForm", new CharacterForm());
		model.addAttribute("badgeList", badgeService.getBadges(childUserId));

		model.addAttribute("currentLimit", spendingService.getCurrentLimit(childUserId));
		model.addAttribute("limitHistory", spendingService.getChildLimitHistory(childUserId));
		model.addAttribute("spendingLimitForm", new SpendingLimitForm());

		model.addAttribute("incomeExpenseForm", incomeExpenseForm);

		if (!bindingResult.hasErrors()) {
			questService.markQuestsViewed(childUserId);
		}

		return TransitionTargetPageNameKeyword.CHILD_HOME_HTML;
	}

	/**
	 * 9. クエスト完了申請 (POST /child/quests/{id}/complete)
	 */
	@PostMapping(TransitionTargetPageNameKeyword.CHILD_QUESTS_COMPLETE)
	public String requestComplete(
			@PathVariable("id") Integer questId,
			@AuthenticationPrincipal CustomUserDetails loginUser) {
		questService.requestComplete(questId, loginUser.getUserId());
		// 🔄 処理完了後は子供一覧にリダイレクト
		return "redirect:" + TransitionTargetPageNameKeyword.CHILD_QUESTS;
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
	
	@ExceptionHandler(IllegalArgumentException.class)
	public String handleIllegalArgumentException(IllegalArgumentException e) {
	    
	    return TransitionTargetPageNameKeyword.ERROR_HTML; 
	}

	
}
