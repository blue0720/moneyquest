package com.example.moneyquest.app.domain.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class IncomeExpenseDto {
	private Integer incomeExpenseId;
	private Integer childUserId;
	private String childUserName;
	private Integer amount;
	private Integer recordType;
	private String category;
	private String memo;
	private LocalDateTime registeredDate;

}
