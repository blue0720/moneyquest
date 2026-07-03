package com.example.moneyquest.app.domain.model;

import java.util.List;

import lombok.Data;

@Data
public class SavingDto {

	private Integer totalIncome;
	private Integer totalExpense;
	private Integer saving;
	private List<String> lavels;
	private List<Integer> monthlyBalance;
	private List<Integer> cumulativeBalance;

}
