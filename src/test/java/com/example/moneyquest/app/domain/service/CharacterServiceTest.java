package com.example.moneyquest.app.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.moneyquest.app.domain.model.CharacterDto;
import com.example.moneyquest.app.infra.entity.CharacterEntity;
import com.example.moneyquest.app.infra.repository.CharacterRepository;

/**
 * CharacterService の単体テスト。
 * レベルアップ計算・カンスト処理・キャラ未存在時の例外ハンドリングを重点的に確認する。
 */
@ExtendWith(MockitoExtension.class)
class CharacterServiceTest {

	@Mock
	private CharacterRepository characterRepository;

	@InjectMocks
	private CharacterService characterService;

	private static final Integer CHILD_USER_ID = 10;

	private CharacterEntity character;

	@BeforeEach
	void setUp() {
		character = new CharacterEntity();
		character.setCharacterId(1);
		character.setCharacterName("タネ");
		character.setLevel(0);
		character.setCurrentExp(0);
		character.setTotalAchievementCount(0);
	}

	@Nested
	@DisplayName("createCharacter")
	class CreateCharacter {

		@Test
		@DisplayName("初期値(レベル0・経験値0・達成数0・名前タネ)でキャラクターが作成される")
		void createCharacter_success() {
			characterService.createCharacter(CHILD_USER_ID);

			verify(characterRepository).save(org.mockito.ArgumentMatchers.argThat(entity ->
					entity.getCharacterName().equals("タネ")
							&& entity.getLevel() == 0
							&& entity.getCurrentExp() == 0
							&& entity.getTotalAchievementCount() == 0
							&& entity.getChildUser().getUserId().equals(CHILD_USER_ID)));
		}
	}

	@Nested
	@DisplayName("getCharacter")
	class GetCharacter {

		@Test
		@DisplayName("キャラクターが存在する場合は次レベル必要経験値を含むDTOを返す")
		void getCharacter_success() {
			when(characterRepository.findByChildUserId(CHILD_USER_ID)).thenReturn(List.of(character));

			CharacterDto dto = characterService.getCharacter(CHILD_USER_ID);

			assertThat(dto.getCharacterName()).isEqualTo("タネ");
			assertThat(dto.getNextLevelExp()).isEqualTo(10);
		}

		@Test
		@DisplayName("キャラクターが存在しない場合はNoSuchElementExceptionを投げる")
		void getCharacter_notFound_throws() {
			when(characterRepository.findByChildUserId(CHILD_USER_ID)).thenReturn(List.of());

			assertThatThrownBy(() -> characterService.getCharacter(CHILD_USER_ID))
					.isInstanceOf(NoSuchElementException.class);
		}
	}

	@Nested
	@DisplayName("updateCharacterName")
	class UpdateCharacterName {

		@Test
		@DisplayName("キャラクターが存在しない場合はNoSuchElementExceptionを投げる")
		void updateCharacterName_notFound_throws() {
			when(characterRepository.findByChildUserId(CHILD_USER_ID)).thenReturn(List.of());

			assertThatThrownBy(() -> characterService.updateCharacterName(CHILD_USER_ID, "新しい名前"))
					.isInstanceOf(NoSuchElementException.class);

			verify(characterRepository, never()).save(any());
		}

		@Test
		@DisplayName("キャラクターが存在する場合は名前が更新される")
		void updateCharacterName_success() {
			when(characterRepository.findByChildUserId(CHILD_USER_ID)).thenReturn(List.of(character));

			characterService.updateCharacterName(CHILD_USER_ID, "新しい名前");

			assertThat(character.getCharacterName()).isEqualTo("新しい名前");
			verify(characterRepository, times(1)).save(character);
		}
	}

	@Nested
	@DisplayName("addExp")
	class AddExp {

		@Test
		@DisplayName("amountがnullの場合は何もしない")
		void addExp_nullAmount_doesNothing() {
			characterService.addExp(CHILD_USER_ID, null);

			verify(characterRepository, never()).findByChildUserId(any());
			verify(characterRepository, never()).save(any());
		}

		@Test
		@DisplayName("amountが0以下の場合は何もしない")
		void addExp_nonPositiveAmount_doesNothing() {
			characterService.addExp(CHILD_USER_ID, 0);
			characterService.addExp(CHILD_USER_ID, -5);

			verify(characterRepository, never()).findByChildUserId(any());
		}

		@Test
		@DisplayName("キャラクターが存在しない場合はNoSuchElementExceptionを投げる")
		void addExp_notFound_throws() {
			when(characterRepository.findByChildUserId(CHILD_USER_ID)).thenReturn(List.of());

			assertThatThrownBy(() -> characterService.addExp(CHILD_USER_ID, 5))
					.isInstanceOf(NoSuchElementException.class);
		}

		@Test
		@DisplayName("必要経験値未満の加算では、レベルは上がらず経験値のみ増える")
		void addExp_belowThreshold_noLevelUp() {
			when(characterRepository.findByChildUserId(CHILD_USER_ID)).thenReturn(List.of(character));

			characterService.addExp(CHILD_USER_ID, 5);

			assertThat(character.getLevel()).isEqualTo(0);
			assertThat(character.getCurrentExp()).isEqualTo(5);
		}

		@Test
		@DisplayName("必要経験値(10)ちょうどでレベルが1つ上がり経験値は0になる")
		void addExp_exactThreshold_levelsUp() {
			when(characterRepository.findByChildUserId(CHILD_USER_ID)).thenReturn(List.of(character));

			characterService.addExp(CHILD_USER_ID, 10);

			assertThat(character.getLevel()).isEqualTo(1);
			assertThat(character.getCurrentExp()).isEqualTo(0);
		}

		@Test
		@DisplayName("大量経験値の加算で複数レベル分アップする")
		void addExp_largeAmount_multipleLevelUps() {
			when(characterRepository.findByChildUserId(CHILD_USER_ID)).thenReturn(List.of(character));

			characterService.addExp(CHILD_USER_ID, 35); // 10ごとに3レベル分 + 端数5

			assertThat(character.getLevel()).isEqualTo(3);
			assertThat(character.getCurrentExp()).isEqualTo(5);
		}

		@Test
		@DisplayName("レベル上限(100)に達したらそれ以上レベルは上がらず経験値は満タン固定になる")
		void addExp_atMaxLevel_caps() {
			character.setLevel(100);
			character.setCurrentExp(0);
			when(characterRepository.findByChildUserId(CHILD_USER_ID)).thenReturn(List.of(character));

			characterService.addExp(CHILD_USER_ID, 1000);

			assertThat(character.getLevel()).isEqualTo(100);
			assertThat(character.getCurrentExp()).isEqualTo(10);
		}

		@Test
		@DisplayName("レベル上限付近で大量に加算すると100で止まる")
		void addExp_nearMaxLevel_capsAtMax() {
			character.setLevel(99);
			character.setCurrentExp(0);
			when(characterRepository.findByChildUserId(CHILD_USER_ID)).thenReturn(List.of(character));

			characterService.addExp(CHILD_USER_ID, 1000);

			assertThat(character.getLevel()).isEqualTo(100);
		}
	}

	@Nested
	@DisplayName("incrementAchievement")
	class IncrementAchievement {

		@Test
		@DisplayName("キャラクターが存在しない場合はNoSuchElementExceptionを投げる")
		void incrementAchievement_notFound_throws() {
			when(characterRepository.findByChildUserId(CHILD_USER_ID)).thenReturn(List.of());

			assertThatThrownBy(() -> characterService.incrementAchievement(CHILD_USER_ID))
					.isInstanceOf(NoSuchElementException.class);
		}

		@Test
		@DisplayName("達成数がnullの場合は1として扱われ1増加する")
		void incrementAchievement_fromNull() {
			character.setTotalAchievementCount(null);
			when(characterRepository.findByChildUserId(CHILD_USER_ID)).thenReturn(List.of(character));

			characterService.incrementAchievement(CHILD_USER_ID);

			assertThat(character.getTotalAchievementCount()).isEqualTo(1);
		}

		@Test
		@DisplayName("達成数が加算される")
		void incrementAchievement_success() {
			character.setTotalAchievementCount(4);
			when(characterRepository.findByChildUserId(CHILD_USER_ID)).thenReturn(List.of(character));

			characterService.incrementAchievement(CHILD_USER_ID);

			assertThat(character.getTotalAchievementCount()).isEqualTo(5);
			verify(characterRepository, times(1)).save(character);
		}
	}
}
