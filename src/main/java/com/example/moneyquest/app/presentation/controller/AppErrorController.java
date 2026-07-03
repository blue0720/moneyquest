package com.example.moneyquest.app.presentation.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.moneyquest.app.presentation.controller.pageproperty.TransitionTargetPageNameKeyword;

@Controller
public class AppErrorController implements ErrorController {

	@GetMapping("/error")
	public String handleError(HttpServletRequest request) {

		// エラーが発生した元のリクエストURIを確認
		String errorUri = (String) request.getAttribute("jakarta.servlet.error.request_uri");

		// /admin 系のURLで発生したエラーは管理者エラー画面へ
		if (errorUri != null && errorUri.startsWith("/admin")) {
			return TransitionTargetPageNameKeyword.ERROR_ADMIN_HTML;
		}

		return TransitionTargetPageNameKeyword.ERROR_HTML;
	}
}
