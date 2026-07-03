package com.example.moneyquest.app.presentation.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class SpendingLimitForm {

	@NotNull
	@Min(0)
	@Max(value = 100000, message = "金額は¥100,000以下の金額を入力してください")
	private Integer limitAmount;
}
