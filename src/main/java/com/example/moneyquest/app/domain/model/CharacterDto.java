package com.example.moneyquest.app.domain.model;

import lombok.Data;

@Data
public class CharacterDto {

	private CharacterType characterType;
	private String characterName;
	private Integer level;
	private Integer currentExp;
	private Integer nextLevelExp;
	private Integer totalAchievementCount;

}
