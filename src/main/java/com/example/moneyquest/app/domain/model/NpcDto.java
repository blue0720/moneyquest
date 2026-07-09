package com.example.moneyquest.app.domain.model;

import lombok.Data;

/**
 * NPC対戦の対戦相手1体分の表示用DTO。
 */
@Data
public class NpcDto {

	private String code;
	private String name;
	private String icon;
	private Integer requiredLevel;
	private Integer coinReward;
	private Integer winProbability;
	private Boolean unlocked;

}
