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
@Table(name = "quest_t")
public class QuestEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "quest_id")
	private Integer questId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "child_user_id")
	private UserEntity childUser;

	@Column(name = "title")
	private String title;

	@Column(name = "description")
	private String description;

	@Column(name = "reward_amount")
	private Integer rewardAmount;

	@Column(name = "exp")
	private Integer exp;

	@Column(name = "limit_amount")
	private Integer limitAmount;

	@Column(name = "status")
	private Integer status;

	@Column(name = "registered_date")
	private LocalDateTime registeredDate;

	@Column(name = "updated_date")
	private LocalDateTime updatedDate;

}
