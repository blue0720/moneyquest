package com.example.moneyquest.app.presentation.form;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * 収支記録に使うコード管理用のクラス
 */
@Data
public class IncomeExpenseForm {
	/** 
	 * 収支ID
	 */
	private Integer incomeExpenseId;

	// エラーメッセージは画面ごとに表示内容を変更するため、HTML側で設定する

	/** 
	 * カテゴリ
	 * 子供の支出必須入力制御はServiceで行う
	 */

	/**
	 * 収支区分
	 * 0:収入 1:支出
	 */
	@NotNull
	private Integer recordType;

	/**
	 * 子供ID
	 * 対象の子供
	 */
	@NotNull
	private Integer childUserId;

	/**
	 * カテゴリ
	 * 支出の分類
	 */

	@Size(max = 20, message = "カテゴリは20文字以内で入力してください")
	private String category;

	/**
	 * 金額
	 */
	@NotNull(message = "金額を入力してください")
	@Min(value = 1, message = "金額は1以上の整数で入力してください")
	@Max(value = 100000, message = "金額は100,000以下の整数で入力してください")
	private Integer amount;

	/**
	 * メモ
	 */
	@Size(max = 50, message = "メモは50文字以内で入力してください")
	private String memo;

	/**
	 * 登録日時
	 * 収入または支出を登録した日時
	 */
	private LocalDateTime registerDate;

}
