package com.example.moneyquest.app.presentation.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.moneyquest.app.domain.service.CustomUserDetails;
import com.example.moneyquest.app.domain.service.QuestService;
import com.example.moneyquest.app.domain.service.QuestTemplateService;
import com.example.moneyquest.app.domain.service.SpendingService;
import com.example.moneyquest.app.domain.service.UserService;
import com.example.moneyquest.app.presentation.common.ParentModelHelper;
import com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword;
import com.example.moneyquest.app.presentation.form.UserForm;

@Controller
public class UserController {

	private final UserService userService;
	private final SpendingService spendingService;
	private final QuestService questService;
	private final QuestTemplateService questTemplateService;

	public UserController(SpendingService spendingService,
			UserService userService,
			QuestService questService,
			QuestTemplateService questTemplateService) {
		this.spendingService = spendingService;
		this.userService = userService;
		this.questService = questService;
		this.questTemplateService = questTemplateService;
	}

	// ========== 保護者：新規登録 ==========

	/** 保護者新規登録画面 */
	@GetMapping(TransitionTargetPageNameKeyword.REGISTER_PARENT)
	public String showRegisterForm(Model model) {
		model.addAttribute("userForm", new UserForm());
		return TransitionTargetPageNameKeyword.REGISTER_PARENT_HTML;
	}

	/** 保護者新規登録処理 */
	@PostMapping(TransitionTargetPageNameKeyword.REGISTER_PARENT)
	public String createParent(@Valid @ModelAttribute UserForm userForm,
			BindingResult bindingResult,
			Model model) {
		validatePasswordConfirm(userForm, bindingResult);
		if (bindingResult.hasErrors()) {
			return TransitionTargetPageNameKeyword.REGISTER_PARENT_HTML;
		}
		try {
			userService.createUser(CustomUserDetails.AUTHORITY_PARENT, userForm);
		} catch (IllegalStateException e) {
			model.addAttribute("errorMessage", e.getMessage());
			return TransitionTargetPageNameKeyword.REGISTER_PARENT_HTML;
		}
		// 登録直後は未ログインのためログイン画面へ
		return "redirect:" + TransitionTargetPageNameKeyword.LOGIN_PARENT;
	}

	//========== 保護者：家族タブ ==========

	/** 家族タブ：保護者本人＋子供一覧 */
	@GetMapping(TransitionTargetPageNameKeyword.PARENT_FAMILY)
	public String showFamilyList(@AuthenticationPrincipal CustomUserDetails loginUser, Model model) {
		ParentModelHelper.setDefaults(model,
				loginUser,
				userService,
				questService, questTemplateService,
				spendingService);

		model.addAttribute("activeTab", "family");
		model.addAttribute("loginUserId", loginUser.getUserId());
		model.addAttribute("familyList", userService.getFamilyByParentId(loginUser.getUserId()));

		return TransitionTargetPageNameKeyword.PARENT_HOME_HTML;
	}

	/** 保護者自身の編集 */
	@PostMapping(TransitionTargetPageNameKeyword.PARENT_FAMILY_EDIT)
	public String updateUser(@AuthenticationPrincipal CustomUserDetails loginUser,
			@ModelAttribute UserForm userForm,
			BindingResult bindingResult,
			Model model) {

		// パスワード入力がある場合だけ確認チェック
		validatePasswordConfirm(userForm, bindingResult);

		if (bindingResult.hasErrors()) {
			setFamilyModel(loginUser, model);
			return TransitionTargetPageNameKeyword.PARENT_HOME_HTML;
		}

		try {
			userService.updateUser(loginUser.getUserId(), userForm);
		} catch (IllegalStateException e) {
			setFamilyModel(loginUser, model);
			model.addAttribute("errorMessage", e.getMessage());
			return TransitionTargetPageNameKeyword.PARENT_HOME_HTML;
		}

		return "redirect:" + TransitionTargetPageNameKeyword.PARENT_FAMILY;
	}

	/** 子供アカウント追加 */
	@PostMapping(TransitionTargetPageNameKeyword.PARENT_FAMILY_CHILD)
	public String createChild(@AuthenticationPrincipal CustomUserDetails loginUser,
			@Valid @ModelAttribute UserForm userForm,
			BindingResult bindingResult,
			Model model) {

		validatePasswordConfirm(userForm, bindingResult);

		if (bindingResult.hasErrors()) {
			setFamilyModel(loginUser, model);
			return TransitionTargetPageNameKeyword.PARENT_HOME_HTML;
		}

		try {
			userService.createChildUser(loginUser.getUserId(), userForm);
		} catch (IllegalStateException e) {
			setFamilyModel(loginUser, model);
			model.addAttribute("errorMessage", e.getMessage());
			return TransitionTargetPageNameKeyword.PARENT_HOME_HTML;
		}

		return "redirect:" + TransitionTargetPageNameKeyword.PARENT_FAMILY;
	}

	/** 子供アカウント編集 */
	@PostMapping(TransitionTargetPageNameKeyword.PARENT_FAMILY_CHILD_EDIT)
	public String updateChild(@PathVariable Integer id,
			@AuthenticationPrincipal CustomUserDetails loginUser,
			@ModelAttribute UserForm userForm,
			BindingResult bindingResult,
			Model model) {

		validatePasswordConfirm(userForm, bindingResult);

		if (bindingResult.hasErrors()) {
			setFamilyModel(loginUser, model);
			return TransitionTargetPageNameKeyword.PARENT_HOME_HTML;
		}

		try {
			userService.updateChildUser(loginUser.getUserId(), id, userForm);
		} catch (IllegalStateException e) {
			setFamilyModel(loginUser, model);
			model.addAttribute("errorMessage", e.getMessage());
			model.addAttribute("editErrorChildId", id);
			return TransitionTargetPageNameKeyword.PARENT_HOME_HTML;
		}

		return "redirect:" + TransitionTargetPageNameKeyword.PARENT_FAMILY;
	}

	/** アカウント削除 */
	@PostMapping(TransitionTargetPageNameKeyword.PARENT_FAMILY_DELETE)
	public String deleteUser(@PathVariable Integer id,
			@AuthenticationPrincipal CustomUserDetails loginUser,
			jakarta.servlet.http.HttpServletRequest request) {

		boolean isSelf = id.equals(loginUser.getUserId());

		userService.deleteFamilyUser(loginUser.getUserId(), id);

		if (isSelf) {
			// 自分を削除した場合はセッションを無効化してタイトルへ
			jakarta.servlet.http.HttpSession session = request.getSession(false);
			if (session != null) {
				session.invalidate();
			}
			return "redirect:" + TransitionTargetPageNameKeyword.TITLE;
		}

		return "redirect:" + TransitionTargetPageNameKeyword.PARENT_FAMILY;
	}

	/** 家族タブ表示用Model共通設定 */
	private void setFamilyModel(CustomUserDetails loginUser, Model model) {
		ParentModelHelper.setDefaults(model,
				loginUser,
				userService,
				questService, questTemplateService,
				spendingService);

		model.addAttribute("activeTab", "family");
		model.addAttribute("loginUserId", loginUser.getUserId());
		model.addAttribute("familyList", userService.getFamilyByParentId(loginUser.getUserId()));
	}
	// ========== 管理者：アカウントタブ(保護者) ==========

	/** 保護者アカウント一覧 */
	@GetMapping(TransitionTargetPageNameKeyword.ADMIN_PARENTS)
	public String showParentList(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			Model model) {
		var parentList = userService.getParentList();
		var adminList = userService.getAdminList();
		model.addAttribute("parentList", parentList);
		model.addAttribute("parentCount", parentList.size());
		model.addAttribute("adminCount", adminList.size());
		model.addAttribute("loginUserId", loginUser.getUserId());
		return TransitionTargetPageNameKeyword.ADMIN_HOME_HTML;
	}

	/** 保護者アカウント削除 */
	@PostMapping(TransitionTargetPageNameKeyword.ADMIN_PARENTS_DELETE)
	public String deleteParent(@PathVariable Integer id) {
		userService.deleteUser(id);
		return "redirect:" + TransitionTargetPageNameKeyword.TITLE;
	}

	// ========== 管理者：アカウントタブ(管理者) ==========

	/** 管理者アカウント一覧 */
	@GetMapping(TransitionTargetPageNameKeyword.ADMIN_ACCOUNTS)
	public String showAdminList(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			Model model) {
		var adminList = userService.getAdminList();
		var parentList = userService.getParentList();
		model.addAttribute("adminList", adminList);
		model.addAttribute("adminCount", adminList.size());
		model.addAttribute("parentCount", parentList.size());
		model.addAttribute("loginUserId", loginUser.getUserId());
		return TransitionTargetPageNameKeyword.ADMIN_HOME_HTML;
	}

	/** 管理者アカウント登録 */
	@PostMapping(TransitionTargetPageNameKeyword.ADMIN_ACCOUNTS)
	public String createAdmin(@Valid @ModelAttribute UserForm userForm,
			BindingResult bindingResult,
			Model model) {
		validatePasswordConfirm(userForm, bindingResult);
		if (bindingResult.hasErrors()) {
			model.addAttribute("adminList", userService.getAdminList());
			return TransitionTargetPageNameKeyword.ADMIN_HOME_HTML;
		}
		try {
			userService.createUser(CustomUserDetails.AUTHORITY_ADMIN, userForm);
		} catch (IllegalStateException e) {
			model.addAttribute("errorMessage", e.getMessage());
			model.addAttribute("adminList", userService.getAdminList());
			return TransitionTargetPageNameKeyword.ADMIN_HOME_HTML;
		}
		return "redirect:" + TransitionTargetPageNameKeyword.ADMIN_ACCOUNTS;
	}

	/** 管理者アカウント編集 */
	@PostMapping(TransitionTargetPageNameKeyword.ADMIN_ACCOUNTS_EDIT)
	public String updateAdmin(
			@AuthenticationPrincipal CustomUserDetails loginUser,
			@PathVariable Integer id,
			@ModelAttribute UserForm userForm,
			BindingResult bindingResult,
			Model model) {

		validateAdminEdit(userForm, bindingResult);

		if (bindingResult.hasErrors()) {
			return "redirect:" + TransitionTargetPageNameKeyword.ADMIN_ACCOUNTS;
		}

		try {
			userService.updateUser(id, userForm);
		} catch (IllegalStateException e) {
			var adminList = userService.getAdminList();
			var parentList = userService.getParentList();
			model.addAttribute("adminList", adminList);
			model.addAttribute("adminCount", adminList.size());
			model.addAttribute("parentCount", parentList.size());
			model.addAttribute("loginUserId", loginUser.getUserId());
			model.addAttribute("errorMessage", e.getMessage());
			model.addAttribute("editErrorAdminId", id);
			return TransitionTargetPageNameKeyword.ADMIN_HOME_HTML;
		}
		return "redirect:" + TransitionTargetPageNameKeyword.ADMIN_ACCOUNTS;
	}

	/** 管理者アカウント削除 */
	@PostMapping(TransitionTargetPageNameKeyword.ADMIN_ACCOUNTS_DELETE)
	public String deleteAdmin(@PathVariable Integer id) {
		userService.deleteUser(id);
		return "redirect:" + TransitionTargetPageNameKeyword.ADMIN_ACCOUNTS;
	}

	/** パスワードと確認用パスワードの一致チェック */
	private void validatePasswordConfirm(UserForm form, BindingResult bindingResult) {
		if (form.getPassword() != null
				&& !form.getPassword().equals(form.getPasswordConfirm())) {
			bindingResult.rejectValue("passwordConfirm", "password.mismatch", "パスワードが一致しません");
		}
	}

	/** 管理者編集用チェック：パスワード未入力なら現在のパスワードを維持する */
	private void validateAdminEdit(UserForm form, BindingResult bindingResult) {
		if (form.getUserName() == null || form.getUserName().isBlank()) {
			bindingResult.rejectValue("userName", "userName.required", "ユーザー名を入力してください");
		} else if (form.getUserName().length() > 20) {
			bindingResult.rejectValue("userName", "userName.size", "ユーザー名は20文字以内で入力してください");
		}

		if (form.getMailAddress() == null || form.getMailAddress().isBlank()) {
			bindingResult.rejectValue("mailAddress", "mailAddress.required", "メールアドレスを入力してください");
		} else if (form.getMailAddress().length() > 100) {
			bindingResult.rejectValue("mailAddress", "mailAddress.size", "メールアドレスは100文字以内で入力してください");
		} else if (!form.getMailAddress().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
			bindingResult.rejectValue("mailAddress", "mailAddress.email", "メールアドレスの形式が正しくありません");
		}

		boolean passwordBlank = form.getPassword() == null || form.getPassword().isBlank();
		boolean passwordConfirmBlank = form.getPasswordConfirm() == null || form.getPasswordConfirm().isBlank();

		if (passwordBlank && passwordConfirmBlank) {
			return;
		}

		if (passwordBlank || passwordConfirmBlank) {
			bindingResult.rejectValue("passwordConfirm", "password.required",
					"パスワードを変更する場合は確認用も入力してください");
			return;
		}

		if (!form.getPassword().matches("^[A-Za-z0-9]{4,20}$")) {
			bindingResult.rejectValue("password", "password.pattern",
					"パスワードは半角英数字4文字以上20文字以内で入力してください");
		}

		if (!form.getPassword().equals(form.getPasswordConfirm())) {
			bindingResult.rejectValue("passwordConfirm", "password.mismatch", "パスワードが一致しません");
		}
	}
}