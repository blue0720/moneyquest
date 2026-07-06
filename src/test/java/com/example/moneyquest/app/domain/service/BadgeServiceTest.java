package com.example.moneyquest.app.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.moneyquest.app.domain.model.BadgeDto;
import com.example.moneyquest.app.domain.model.CharacterDto;

/**
 * BadgeService の単体テスト。
 * 達成数のしきい値に応じたバッジ獲得判定と、CharacterService依存の例外伝播を確認する。
 */
@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

	@Mock
	private CharacterService characterService;

	@InjectMocks
	private BadgeService badgeService;

	private static final Integer CHILD_USER_ID = 10;

	private CharacterDto characterWithCount(Integer count) {
		CharacterDto dto = new CharacterDto();
		dto.setTotalAchievementCount(count);
		return dto;
	}

	@Test
	@DisplayName("達成数0の場合はすべてのバッジが未獲得になる")
	void getBadges_zeroCount_noneEarned() {
		when(characterService.getCharacter(CHILD_USER_ID)).thenReturn(characterWithCount(0));

		List<BadgeDto> badges = badgeService.getBadges(CHILD_USER_ID);

		assertThat(badges).allMatch(b -> !b.getEarned());
	}

	@Test
	@DisplayName("達成数がnullの場合は0として扱われる")
	void getBadges_nullCount_treatedAsZero() {
		when(characterService.getCharacter(CHILD_USER_ID)).thenReturn(characterWithCount(null));

		List<BadgeDto> badges = badgeService.getBadges(CHILD_USER_ID);

		assertThat(badges).allMatch(b -> !b.getEarned());
	}

	@Test
	@DisplayName("達成数1の場合ははじめの一歩のみ獲得済みになる")
	void getBadges_countOne_firstBadgeEarned() {
		when(characterService.getCharacter(CHILD_USER_ID)).thenReturn(characterWithCount(1));

		List<BadgeDto> badges = badgeService.getBadges(CHILD_USER_ID);

		BadgeDto firstStep = badges.stream().filter(b -> b.getCode().equals("first_step")).findFirst().orElseThrow();
		assertThat(firstStep.getEarned()).isTrue();

		BadgeDto hardWorker = badges.stream().filter(b -> b.getCode().equals("hard_worker")).findFirst().orElseThrow();
		assertThat(hardWorker.getEarned()).isFalse();
	}

	@Test
	@DisplayName("達成数100(最大しきい値)の場合はすべてのバッジが獲得済みになる")
	void getBadges_countAtMax_allEarned() {
		when(characterService.getCharacter(CHILD_USER_ID)).thenReturn(characterWithCount(100));

		List<BadgeDto> badges = badgeService.getBadges(CHILD_USER_ID);

		assertThat(badges).allMatch(BadgeDto::getEarned);
		assertThat(badges).hasSize(6);
	}

	@Test
	@DisplayName("CharacterServiceが例外を投げた場合はそのまま呼び出し元に伝播する")
	void getBadges_characterServiceThrows_propagates() {
		when(characterService.getCharacter(CHILD_USER_ID))
				.thenThrow(new java.util.NoSuchElementException("Character not found"));

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> badgeService.getBadges(CHILD_USER_ID))
				.isInstanceOf(java.util.NoSuchElementException.class);
	}
}
