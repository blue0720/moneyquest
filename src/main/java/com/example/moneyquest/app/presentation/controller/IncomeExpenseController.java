package com.example.moneyquest.app.presentation.controller;

import java.beans.PropertyEditorSupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.moneyquest.app.domain.service.CharacterService;
import com.example.moneyquest.app.domain.service.CustomUserDetails;
import com.example.moneyquest.app.domain.service.GraphService;
import com.example.moneyquest.app.domain.service.IncomeExpenseService;
import com.example.moneyquest.app.domain.service.QuestService;
import com.example.moneyquest.app.domain.service.QuestTemplateService;
import com.example.moneyquest.app.domain.service.SpendingService;
import com.example.moneyquest.app.domain.service.UserService;
import com.example.moneyquest.app.infra.entity.UserEntity;
import com.example.moneyquest.app.infra.repository.UserRepository;
import com.example.moneyquest.app.presentation.common.ParentModelHelper;
import com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword;
import com.example.moneyquest.app.presentation.form.IncomeExpenseForm;

@Controller
public class IncomeExpenseController {

	private final IncomeExpenseService incomeExpenseService;
	private final GraphService graphService;
	private final CharacterService characterService;
	private final SpendingService spendingService;
	private final UserService userService;
	private final QuestService questService;
	private final QuestTemplateService questTemplateService;
	private final UserRepository userRepository;

	public IncomeExpenseController(
			IncomeExpenseService incomeExpenseService,
			GraphService graphService,
			CharacterService characterService,
			SpendingService spendingService,
			UserService userService,
			QuestService questService,
			QuestTemplateService questTemplateService,
			UserRepository userRepository) {
		this.incomeExpenseService = incomeExpenseService;
		this.graphService = graphService;
		this.characterService = characterService;
		this.spendingService = spendingService;
		this.userService = userService;
		this.questService = questService;
		this.questTemplateService = questTemplateService;
		this.userRepository = userRepository;
	}

	// 一覧 権限で判別
	@GetMapping({ TransitionTargetPageNameKeyword.CHILD_RECORDS,
			TransitionTargetPageNameKeyword.PARENT_INCOME })
	public String incomeExpenseList(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			Model model) {

		model.addAttribute("activeTab", "record");

		if (loginUser.getUser().getAuthority() == CustomUserDetails.AUTHORITY_CHILD) {
			// 子供の場合、自分の支出一覧を表示
			setChildHomeData(loginUser, model);
			model.addAttribute("incomeExpenseList",
					incomeExpenseService.getRecords(loginUser.getUserId()));
			return TransitionTargetPageNameKeyword.CHILD_HOME_HTML;
		} else {
			//保護者の場合、全子供アカウントの収入一覧を表示
			setParentHomeData(loginUser, model, true);
			return TransitionTargetPageNameKeyword.PARENT_HOME_HTML;

		}
	}

	// 登録
	@PostMapping({ TransitionTargetPageNameKeyword.CHILD_RECORDS,
			TransitionTargetPageNameKeyword.PARENT_INCOME })
	public String createIncomeExpense(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			@Valid IncomeExpenseForm form,
			BindingResult result,
			Model model) {

		UserEntity user = loginUser.getUser();

		if (result.hasErrors()) {

			model.addAttribute("incomeExpenseForm", form);
			model.addAttribute("activeTab", "record");
			model.addAttribute("showIncomeAddModal", true);

			if (user.getAuthority() == CustomUserDetails.AUTHORITY_CHILD) {
				setChildHomeData(loginUser, model);
				model.addAttribute(
						"incomeExpenseList",
						incomeExpenseService.getExpenseRecords(loginUser.getUserId()));

				return "redirect:" + TransitionTargetPageNameKeyword.CHILD_RECORDS;
			} else {

				setParentHomeData(loginUser, model, false);
				return "redirect:" + TransitionTargetPageNameKeyword.PARENT_INCOME;
			}
		}

		Integer targetChildUserId = null;

		if (user.getAuthority() != CustomUserDetails.AUTHORITY_CHILD) {
			targetChildUserId = form.getChildUserId();
		}

		try {
			incomeExpenseService.createRecord(form, user, targetChildUserId);
		} catch (SecurityException e) {
			return "redirect:" + TransitionTargetPageNameKeyword.ERROR;
		}

		if (user.getAuthority() == CustomUserDetails.AUTHORITY_CHILD) {
			return "redirect:" + TransitionTargetPageNameKeyword.CHILD_RECORDS;
		} else {
			return "redirect:" + TransitionTargetPageNameKeyword.PARENT_INCOME;
		}
	}

	// 編集
	@PostMapping({ TransitionTargetPageNameKeyword.CHILD_RECORDS_EDIT,
			TransitionTargetPageNameKeyword.PARENT_INCOME_EDIT })
	public String updateIncomeExpense(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			@PathVariable Integer id,
			@Valid IncomeExpenseForm form,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {

		UserEntity user = loginUser.getUser();

		if (result.hasErrors()) {

			model.addAttribute("incomeExpenseForm", form);
			model.addAttribute("activeTab", "record");
			model.addAttribute("showIncomeEditModal", true);

			if (user.getAuthority() == CustomUserDetails.AUTHORITY_CHILD) {
				setChildHomeData(loginUser, model);
				return "redirect:" + TransitionTargetPageNameKeyword.CHILD_RECORDS;

			} else {

				setParentHomeData(loginUser, model, false);
				return "redirect:" + TransitionTargetPageNameKeyword.PARENT_INCOME;
			}
		}

		form.setIncomeExpenseId(id);

		try {
			incomeExpenseService.updateRecord(form, user);
		} catch (SecurityException e) {
			return "redirect:" + TransitionTargetPageNameKeyword.ERROR;
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
			if (user.getAuthority() == CustomUserDetails.AUTHORITY_CHILD) {
				return "redirect:" + TransitionTargetPageNameKeyword.CHILD_RECORDS;
			} else {
				return "redirect:" + TransitionTargetPageNameKeyword.PARENT_INCOME;
			}
		}

		// 正常時
		if (user.getAuthority() == CustomUserDetails.AUTHORITY_CHILD) {
			return "redirect:" + TransitionTargetPageNameKeyword.CHILD_RECORDS;
		} else {
			return "redirect:" + TransitionTargetPageNameKeyword.PARENT_INCOME;
		}
	}

	// 削除
	@PostMapping({ TransitionTargetPageNameKeyword.CHILD_RECORDS_DELETE,
			TransitionTargetPageNameKeyword.PARENT_INCOME_DELETE })
	public String deleteIncomeExpense(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			@PathVariable Integer id) {

		UserEntity user = loginUser.getUser();

		try {
			incomeExpenseService.deleteRecord(id, user);
		} catch (SecurityException e) {
			return "redirect:" + TransitionTargetPageNameKeyword.ERROR;
		}

		if (user.getAuthority() == CustomUserDetails.AUTHORITY_CHILD) {
			return "redirect:" + TransitionTargetPageNameKeyword.CHILD_RECORDS;
		} else {
			return "redirect:" + TransitionTargetPageNameKeyword.PARENT_INCOME;
		}
	}

	// 子供ホーム画面に必要な共通属性をセット
	private void setChildHomeData(CustomUserDetails loginUser, Model model) {
		Integer childUserId = loginUser.getUserId();
		model.addAttribute("userName", loginUser.getUser().getUserName());
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
	}

	private void setParentHomeData(
			CustomUserDetails loginUser,
			Model model,
			boolean setDefaultForm) {

		List<UserEntity> children = userRepository.findByParentUserId(loginUser.getUserId());

		Map<Integer, Integer> currentMoneyMap = new HashMap<>();

		for (UserEntity child : children) {
			currentMoneyMap.put(
					child.getUserId(),
					graphService.calcSavings(child.getUserId()));
		}

		ParentModelHelper.setDefaults(model, loginUser, userService, questService, questTemplateService,
				spendingService);
		model.addAttribute("childList", children);
		model.addAttribute("currentMoneyMap", currentMoneyMap);
		if (setDefaultForm) {
			model.addAttribute("incomeExpenseForm", new IncomeExpenseForm());
		}
		model.addAttribute(
				"incomeExpenseList",
				incomeExpenseService.getParentIncomeRecords(
						loginUser.getUserId()));
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