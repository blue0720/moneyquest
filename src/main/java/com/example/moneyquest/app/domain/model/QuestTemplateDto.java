package com.example.moneyquest.app.domain.model;

import lombok.Data;

@Data
public class QuestTemplateDto {

	private Integer questTemplateId;
	private String title;
	private String description;
	private Integer rewardAmount;
	private Integer exp;

}
