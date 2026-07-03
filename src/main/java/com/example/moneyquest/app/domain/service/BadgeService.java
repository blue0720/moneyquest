package com.example.moneyquest.app.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.moneyquest.app.domain.model.BadgeDto;

/**
 * バッジ/称号サービス。
 * character_t.total_achievement_count（クエスト達成数）のしきい値をもとに、
 * 獲得済みバッジ一覧を動的に判定して返す（バッジ専用テーブルは持たない）。
 */
@Service
public class BadgeService {

	private record BadgeDefinition(String code, String name, String description, String icon, int requiredCount) {
	}

	private static final List<BadgeDefinition> DEFINITIONS = List.of(
			new BadgeDefinition("first_step", "はじめの一歩", "はじめてクエストをたっせいした", "🌱", 1),
			new BadgeDefinition("hard_worker", "がんばりやさん", "クエストを5かいたっせいした", "🔥", 5),
			new BadgeDefinition("steady_master", "コツコツマスター", "クエストを10かいたっせいした", "⭐", 10),
			new BadgeDefinition("veteran", "ベテランクエスター", "クエストを30かいたっせいした", "🎖️", 30),
			new BadgeDefinition("expert", "クエストのたつじん", "クエストを50かいたっせいした", "👑", 50),
			new BadgeDefinition("legend", "でんせつのクエスター", "クエストを100かいたっせいした", "🏆", 100));

	private final CharacterService characterService;

	public BadgeService(CharacterService characterService) {
		this.characterService = characterService;
	}

	/**
	 * 指定した子供の全バッジ一覧を、獲得済みかどうかのフラグ付きで返す（未獲得のものも含む）。
	 */
	public List<BadgeDto> getBadges(Integer childUserId) {
		Integer achievementCount = characterService.getCharacter(childUserId).getTotalAchievementCount();
		int count = achievementCount == null ? 0 : achievementCount;

		return DEFINITIONS.stream()
				.map(def -> toDto(def, count))
				.toList();
	}

	private BadgeDto toDto(BadgeDefinition def, int achievementCount) {
		BadgeDto dto = new BadgeDto();
		dto.setCode(def.code());
		dto.setName(def.name());
		dto.setDescription(def.description());
		dto.setIcon(def.icon());
		dto.setRequiredCount(def.requiredCount());
		dto.setEarned(achievementCount >= def.requiredCount());
		return dto;
	}
}
