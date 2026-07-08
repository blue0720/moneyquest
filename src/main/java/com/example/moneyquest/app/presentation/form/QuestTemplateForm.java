package com.example.moneyquest.app.presentation.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class QuestTemplateForm {

	private Integer questTemplateId;

	@NotNull(message = "タイトルは必須です")
	@Size(max = 30, message = "タイトルは30文字以内で入力してください")
	private String title;

	@Size(max = 50, message = "メモは50文字以内で入力してください")
	private String description;

	@NotNull(message = "お小遣いは必須です")
	@Min(value = 1, message = "¥1以上の金額を入力してください")
	@Max(value = 100000, message = "金額は¥100,000以下の金額で入力してください")
	private Integer rewardAmount;

	private Integer exp;
	private Integer childUserId;
}