package com.example.moneyquest.app.infra.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "income_expense_t")
public class IncomeExpenseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "income_Expense_id")
	private Integer incomeExpenseId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "child_user_id")
	private UserEntity childUser;

	@Column(name = "record_type")
	private Integer recordType;

	@Column(name = "category")
	private String category;

	@Column(name = "amount")
	private Integer amount;

	@Column(name = "memo")
	private String memo;

	@Column(name = "registered_date")
	private LocalDateTime registeredDate;

}
