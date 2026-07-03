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
@Table(name = "spending_limit_t")
public class SpendingLimitEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "spending_limit_id")
	private Integer spendingLimitId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "child_user_id")
	private UserEntity childUser;

	@Column(name = "limit_amount")
	private Integer limitAmount;

	@Column(name = "request_status")
	private Integer requestStatus;

	@Column(name = "registered_date")
	private LocalDateTime registeredDate;

	@Column(name = "updated_date")
	private LocalDateTime updatedDate;

}
