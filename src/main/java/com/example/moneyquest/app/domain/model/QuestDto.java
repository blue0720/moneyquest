package com.example.moneyquest.app.domain.model;

import lombok.Data;

@Data
public class QuestDto {

	private Integer questId;
	private String title;
	private String description;
	private Integer rewardAmount;
	private Integer status;
	private String childName;

}
