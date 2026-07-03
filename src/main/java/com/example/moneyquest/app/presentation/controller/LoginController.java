package com.example.moneyquest.app.presentation.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword;

@Controller
public class LoginController {

	/** タイトル画面 */
	@GetMapping({"/", TransitionTargetPageNameKeyword.TITLE})
	public String title() {
		return TransitionTargetPageNameKeyword.TITLE_HTML;
	}

	/** 保護者ログイン画面（共通 login.html） */
	@GetMapping(TransitionTargetPageNameKeyword.LOGIN_PARENT)
	public String parentLogin() {
		return TransitionTargetPageNameKeyword.LOGIN_HTML;
	}

	/** 子供ログイン画面（共通 login.html） */
	@GetMapping(TransitionTargetPageNameKeyword.LOGIN_CHILD)
	public String childLogin() {
		return TransitionTargetPageNameKeyword.LOGIN_HTML;
	}

	/** 管理者ログイン画面（共通 login.html） */
	@GetMapping(TransitionTargetPageNameKeyword.LOGIN_ADMIN)
	public String adminLogin() {
		return TransitionTargetPageNameKeyword.LOGIN_HTML;
	}

	/** ログアウト */
	@GetMapping(TransitionTargetPageNameKeyword.LOGOUT)
	public String logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return "redirect:/";
	}

//	/** 保護者・子供用セッションエラー画面 */
//	@GetMapping(TransitionTargetPageNameKeyword.ERROR)
//	public String userError() {
//		return TransitionTargetPageNameKeyword.ERROR_HTML;
//	}

	/** 管理者用セッションエラー画面 */
	@GetMapping(TransitionTargetPageNameKeyword.ERROR_ADMIN)
	public String adminError() {
		return TransitionTargetPageNameKeyword.ERROR_ADMIN_HTML;
	}
}