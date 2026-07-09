package com.example.moneyquest.app.domain.model;

import lombok.Data;

/**
 * NPC対戦1回分の結果。永続化はせず、対戦直後に画面へ渡す用途のみ。
 */
@Data
public class BattleResultDto {

	private String npcName;
	private String npcIcon;
	private Boolean win;
	private Integer coinsAwarded;
	private Integer winProbability;

}
