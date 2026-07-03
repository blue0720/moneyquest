package com.example.moneyquest.app.infra.entity;

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
@Table(name = "quest_template_t")
public class QuestTemplateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "quest_template_id")
	private Integer questTemplateId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_user_id")
	private UserEntity parentUser;

	@Column(name = "title")
	private String title;

	@Column(name = "description")
	private String description;

	@Column(name = "reward_amount")
	private Integer rewardAmount;

	@Column(name = "exp")
	private Integer exp;

}
