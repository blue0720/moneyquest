package com.example.moneyquest.app.domain.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.moneyquest.app.domain.model.BattleResultDto;
import com.example.moneyquest.app.domain.model.CharacterDto;
import com.example.moneyquest.app.domain.model.NpcDto;

/**
 * NPC対戦サービス。
 * NPCロースターはバッジ機能と同様にハードコードで持ち、専用テーブルは作らない。
 * 対戦は何回でも挑戦・報酬受け取り可能（周回可）で、結果も永続化しない。
 */
@Service
public class BattleService {

	private static final int BASE_WIN_PROBABILITY = 50;
	private static final int PROBABILITY_PER_LEVEL_DIFF = 5;
	private static final int MAX_WIN_PROBABILITY = 90;

	private record NpcDefinition(String code, String name, String icon, int requiredLevel, int coinReward) {
	}

	private static final List<NpcDefinition> DEFINITIONS = List.of(
			new NpcDefinition("SLIME", "スライムン", "🟢", 0, 5),
			new NpcDefinition("GOBLIN", "ゴブジャー", "👺", 10, 10),
			new NpcDefinition("GOLEM", "ロックゴーレム", "🪨", 25, 20),
			new NpcDefinition("WOLF", "フレイムウルフ", "🐺", 40, 35),
			new NpcDefinition("DRAGON", "ドラゴニア", "🐉", 70, 60));

	private final CharacterService characterService;
	/** 1〜100の乱数を返す。勝敗判定に使用。テストから結果を固定できるよう差し替え可能にしている。 */
	private final IntSupplier diceRoller;

	@Autowired
	public BattleService(CharacterService characterService) {
		this(characterService, () -> ThreadLocalRandom.current().nextInt(1, 101));
	}

	BattleService(CharacterService characterService, IntSupplier diceRoller) {
		this.characterService = characterService;
		this.diceRoller = diceRoller;
	}

	/**
	 * 指定した子供に対する全NPCの一覧を、解放状態・勝率付きで返す。
	 */
	public List<NpcDto> getNpcList(Integer childUserId) {
		int level = getLevel(childUserId);
		return DEFINITIONS.stream()
				.map(def -> toDto(def, level))
				.toList();
	}

	/**
	 * NPCに挑戦する。解放前のNPCまたは存在しないNPCコードの場合は例外。
	 * 勝利したら勝利報酬コインをキャラクターに加算する。
	 */
	public BattleResultDto challenge(Integer childUserId, String npcCode) {
		int level = getLevel(childUserId);
		NpcDefinition def = DEFINITIONS.stream()
				.filter(d -> d.code().equals(npcCode))
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException("対象のNPCが見つかりません。"));

		if (level < def.requiredLevel()) {
			throw new IllegalArgumentException("このNPCとはまだ対戦できません。");
		}

		int winProbability = calcWinProbability(level, def.requiredLevel());
		boolean win = diceRoller.getAsInt() <= winProbability;

		BattleResultDto result = new BattleResultDto();
		result.setNpcName(def.name());
		result.setNpcIcon(def.icon());
		result.setWin(win);
		result.setWinProbability(winProbability);

		if (win) {
			characterService.addCoins(childUserId, def.coinReward());
			result.setCoinsAwarded(def.coinReward());
		} else {
			result.setCoinsAwarded(0);
		}

		return result;
	}

	private NpcDto toDto(NpcDefinition def, int level) {
		boolean unlocked = level >= def.requiredLevel();

		NpcDto dto = new NpcDto();
		dto.setCode(def.code());
		dto.setName(def.name());
		dto.setIcon(def.icon());
		dto.setRequiredLevel(def.requiredLevel());
		dto.setCoinReward(def.coinReward());
		dto.setUnlocked(unlocked);
		dto.setWinProbability(unlocked ? calcWinProbability(level, def.requiredLevel()) : 0);
		return dto;
	}

	private int calcWinProbability(int level, int requiredLevel) {
		int probability = BASE_WIN_PROBABILITY + (level - requiredLevel) * PROBABILITY_PER_LEVEL_DIFF;
		return Math.min(MAX_WIN_PROBABILITY, probability);
	}

	private int getLevel(Integer childUserId) {
		CharacterDto character = characterService.getCharacter(childUserId);
		return character.getLevel() == null ? 0 : character.getLevel();
	}

}
