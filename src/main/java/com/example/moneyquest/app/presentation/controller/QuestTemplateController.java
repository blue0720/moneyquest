package com.example.moneyquest.app.presentation.controller;

import java.beans.PropertyEditorSupport;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.moneyquest.app.domain.service.CustomUserDetails;
import com.example.moneyquest.app.domain.service.QuestService;
import com.example.moneyquest.app.domain.service.QuestTemplateService;
import com.example.moneyquest.app.domain.service.SpendingService;
import com.example.moneyquest.app.domain.service.UserService;
import com.example.moneyquest.app.presentation.common.ParentModelHelper;
import com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword;
import com.example.moneyquest.app.presentation.form.QuestSendForm;
import com.example.moneyquest.app.presentation.form.QuestTemplateForm;

@Controller
public class QuestTemplateController {

	private final QuestTemplateService questTemplateService;
	private final UserService userService;
	private final QuestService questService;
	private final SpendingService spendingService;

	public QuestTemplateController(
			QuestTemplateService questTemplateService,
			UserService userService,
			QuestService questService,
			SpendingService spendingService) {
		this.questTemplateService = questTemplateService;
		this.userService = userService;
		this.questService = questService;
		this.spendingService = spendingService;
	}

	@GetMapping(TransitionTargetPageNameKeyword.PARENT_TEMPLATES)
	public String showTemplates(
			@AuthenticationPrincipal CustomUserDetails loginUser, Model model) {

		Integer parentUserId = loginUser.getUserId();

		var templateList = questTemplateService.findByParentUserId(parentUserId);
		var childList = userService.getChildrenByParentId(parentUserId);

		ParentModelHelper.setDefaults(model,
				loginUser,
				userService,
				questService, questTemplateService,
				spendingService);
		model.addAttribute("userName", loginUser.getUser().getUserName());
		model.addAttribute("questApprovalCount", 0);
		model.addAttribute("limitApprovalCount", 0);
		model.addAttribute("templateCount", templateList.size());
		model.addAttribute("templateList", templateList);
		model.addAttribute("childList", childList);
		model.addAttribute("questTemplateForm", new QuestTemplateForm());
		model.addAttribute("activeTab", "template");

		return TransitionTargetPageNameKeyword.PARENT_HOME_HTML;
	}

	@PostMapping(TransitionTargetPageNameKeyword.PARENT_TEMPLATES)
	public String addTemplate(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			@Validated QuestTemplateForm form,
			BindingResult bindingResult,
			Model model) {

		if (bindingResult.hasErrors()) {
			model.addAttribute("templateErrorMessage", "入力内容に誤りがあります。");
			return showTemplates(loginUser, model);
		}

		questTemplateService.addTemplate(loginUser.getUserId(), form);
		return "redirect:" + TransitionTargetPageNameKeyword.PARENT_TEMPLATES;
	}

	@PostMapping(TransitionTargetPageNameKeyword.PARENT_TEMPLATES_EDIT)
	public String updateTemplate(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			@PathVariable Integer id,
			@Validated QuestTemplateForm form,
			BindingResult bindingResult,
			Model model) {

		form.setQuestTemplateId(id);

		if (bindingResult.hasErrors()) {
			model.addAttribute("templateErrorMessage", "入力内容に誤りがあります。");
			return showTemplates(loginUser, model);
		}

		questTemplateService.updateTemplate(loginUser.getUserId(), form);
		return "redirect:" + TransitionTargetPageNameKeyword.PARENT_TEMPLATES;
	}

	@PostMapping(TransitionTargetPageNameKeyword.PARENT_TEMPLATES_DELETE)
	public String deleteTemplate(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			@PathVariable Integer id) {

		questTemplateService.deleteTemplate(loginUser.getUserId(), id);
		return "redirect:" + TransitionTargetPageNameKeyword.PARENT_TEMPLATES;
	}

	@PostMapping(TransitionTargetPageNameKeyword.PARENT_TEMPLATES_ADD)
	public String addQuestFromTemplate(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			@Validated QuestSendForm form,
			BindingResult bindingResult,
			Model model) {

		if (bindingResult.hasErrors()) {
			model.addAttribute("templateErrorMessage", "入力内容に誤りがあります。");
			return showTemplates(loginUser, model);
		}

		Integer parentUserId = loginUser.getUser().getUserId();

		if (form.getChildUserId() != null && form.getChildUserId() == 0) {
			var childList = userService.getChildrenByParentId(parentUserId);

			for (var child : childList) {
				QuestSendForm childForm = new QuestSendForm();

				childForm.setChildUserId(child.getUserId());
				childForm.setTitle(form.getTitle());
				childForm.setRewardAmount(form.getRewardAmount());
				childForm.setExp(form.getExp());
				childForm.setDescription(form.getDescription());

				questService.createQuest(childForm,loginUser.getUser().getUserId());
			}
		} else {
			questService.createQuest(form,loginUser.getUser().getUserId());
		}

		return "redirect:" + TransitionTargetPageNameKeyword.PARENT_TEMPLATES;
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