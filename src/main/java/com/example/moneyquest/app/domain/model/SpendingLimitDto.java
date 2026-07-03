package com.example.moneyquest.app.domain.model;

import lombok.Data;

@Data
public class SpendingLimitDto {

	private Integer spendingLimitId;
	private Integer limitAmount;
	private Integer requestStatus;
	

	 // 追加
	private Integer currentExpenseAmount;//最新に月の支出
	private String childUserName;        //子供の名前
    private String targetMonth;          // 例: 2026-06
    private Integer monthlyExpenseAmount;// その月の支出
    private Boolean achieved;            // 達成/未達成
}
