package com.example.moneyquest.app.presentation.controller.pageproperty;

public class TransitionTargetPageNameKeyword {

	/** 
	
	システムで利用しているHTMLおよびURLパターンのリストを管理するクラス 
	
	役割プレフィックス（/login/* , /child/* , /parent/* , /admin/*）で 
	
	URLを分離し、Spring Securityでアクセス制御を行う。 
	
	*/

	// ============================================================ 
	// HTML（テンプレート名） 
	// ============================================================ 

	// ----- ログイン前 ----- 
	public static final String TITLE_HTML = "title";
	public static final String LOGIN_HTML = "login";
	public static final String REGISTER_PARENT_HTML = "parent-register";
	public static final String ERROR_HTML = "user-session-error";
	public static final String ERROR_ADMIN_HTML = "admin-session-error";

	// ----- 子供 ----- 
	public static final String CHILD_HOME_HTML = "child-home";

	// ----- 保護者 ----- 
	public static final String PARENT_HOME_HTML = "parent-home";

	// ----- 管理者 ----- 
	public static final String ADMIN_HOME_HTML = "admin-home";

	// ============================================================ 
	// URLパターン（Controller） 
	// ============================================================ 

	// ----- 認証・共通 ----- 
	/** タイトル */
	public static final String TITLE = "/title";
	
	/** ログイン */
	public static final String LOGIN_PARENT = "/parent/login";
	public static final String LOGIN_CHILD = "/child/login";
	public static final String LOGIN_ADMIN = "/admin/login";
	
	/** ログアウト */
	public static final String LOGOUT = "/logout";
	
	/** 保護者新規登録 */
	public static final String REGISTER_PARENT = "/register/parent";
	
	/** エラー */
	public static final String ERROR = "/error";
	public static final String ERROR_ADMIN = "/admin/error";

	// ----- 子供 ----- 
	/** ホームタブ */
	public static final String CHILD_HOME = "/child/home";
	public static final String CHILD_CHARACTER_NAME = "/child/character/name";
	public static final String CHILD_CHARACTER_TYPE = "/child/character/type";

	/** クエストタブ */
	public static final String CHILD_QUESTS = "/child/quest";
	/** {id} = quest_id */
	public static final String CHILD_QUESTS_COMPLETE = "/child/quest/{id}/complete";

	/** きろくタブ */
	public static final String CHILD_RECORDS = "/child/records";
	/** {id} = income_expense_id */
	public static final String CHILD_RECORDS_EDIT = "/child/records/{id}/edit";
	public static final String CHILD_RECORDS_DELETE = "/child/records/{id}/delete";

	/** 収支・グラフタブ */
	public static final String CHILD_GRAPH = "/child/graph";

	/** 上限申請タブ */
	public static final String CHILD_LIMIT = "/child/limit";

	// ----- 保護者 ----- 
	/** ホームタブ */
	public static final String PARENT_HOME = "/parent/home";

	/** グラフタブ */
	public static final String PARENT_GRAPH = "/parent/graph";
	public static final String PARENT_BALANCE = "/parent/balance";

	/** 家族タブ */
	public static final String PARENT_FAMILY = "/parent/family";
	public static final String PARENT_FAMILY_EDIT = "/parent/family/edit";
	public static final String PARENT_FAMILY_CHILD = "/parent/family/child";
	/** {id} = user_id */
	public static final String PARENT_FAMILY_CHILD_EDIT = "/parent/family/child/{id}/edit";
	public static final String PARENT_FAMILY_DELETE = "/parent/family/{id}/delete";

	/** 上限承認タブ */
	public static final String PARENT_LIMIT = "/parent/limit";
	/** {id} = spending_limit_id */
	public static final String PARENT_LIMIT_APPROVE = "/parent/limit/{id}/approve";
	public static final String PARENT_LIMIT_REJECT = "/parent/limit/{id}/reject";

	/** クエストタブ */
	public static final String PARENT_QUESTS = "/parent/quest";
	/** {id} = quest_id */
	public static final String PARENT_QUESTS_EDIT = "/parent/quest/{id}/edit";
	public static final String PARENT_QUESTS_DELETE = "/parent/quest/{id}/delete";

	/** クエストテンプレートタブ */
	public static final String PARENT_TEMPLATES = "/parent/templates";
	/** {id} = quest_template_id */
	public static final String PARENT_TEMPLATES_EDIT = "/parent/templates/{id}/edit";
	public static final String PARENT_TEMPLATES_DELETE = "/parent/templates/{id}/delete";
	public static final String PARENT_TEMPLATES_ADD = "/parent/templates/add-quest";

	/** クエスト承認タブ */
	public static final String PARENT_APPROVALS = "/parent/approvals";
	/** {id} = quest_id */
	public static final String PARENT_APPROVALS_APPROVE = "/parent/approvals/{id}/approve";
	public static final String PARENT_APPROVALS_REJECT = "/parent/approvals/{id}/reject";

	/** 収入タブ */
	public static final String PARENT_INCOME = "/parent/income";
	/** {id} = income_expense_id */
	public static final String PARENT_INCOME_EDIT = "/parent/income/{id}/edit";
	public static final String PARENT_INCOME_DELETE = "/parent/income/{id}/delete";

	// ----- 管理者 ----- 
	/** 保護者アカウント管理タブ */
	public static final String ADMIN_PARENTS = "/admin/parents";
	/** {id} = user_id */
	public static final String ADMIN_PARENTS_DELETE = "/admin/parents/{id}/delete";

	/** 管理者アカウント管理タブ */
	public static final String ADMIN_ACCOUNTS = "/admin/accounts";
	/** {id} = user_id */
	public static final String ADMIN_ACCOUNTS_EDIT = "/admin/accounts/{id}/edit";
	public static final String ADMIN_ACCOUNTS_DELETE = "/admin/accounts/{id}/delete";

	private TransitionTargetPageNameKeyword() {
		// インスタンス化禁止 
	}

}
