package com.example.moneyquest.app.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.moneyquest.app.domain.model.BattleResultDto;
import com.example.moneyquest.app.domain.model.CharacterDto;
import com.example.moneyquest.app.domain.model.NpcDto;

/**
 * BattleService の単体テスト。
 * NPC解放判定・勝率計算・勝敗判定(乱数固定)・勝利時のコイン加算を重点的に確認する。
 */
@ExtendWith(MockitoExtension.class)
class BattleServiceTest {

	@Mock
	private CharacterService characterService;

	private static final Integer CHILD_USER_ID = 10;

	private BattleService battleServiceWithRoll(int fixedRoll) {
		return new BattleService(characterService, () -> fixedRoll);
	}

	private void mockLevel(int level) {
		CharacterDto dto = new CharacterDto();
		dto.setLevel(level);
		when(characterService.getCharacter(CHILD_USER_ID)).thenReturn(dto);
	}

	@BeforeEach
	void setUp() {
		// デフォルトは何もしない。各テストでmockLevelを呼ぶ。
	}

	@Nested
	@DisplayName("getNpcList")
	class GetNpcList {

		@Test
		@DisplayName("レベル0では最初のNPC(SLIME)のみ解放され、勝率は基準値(50%)になる")
		void getNpcList_lowLevel_onlyFirstUnlocked() {
			mockLevel(0);

			List<NpcDto> npcList = battleServiceWithRoll(1).getNpcList(CHILD_USER_ID);

			NpcDto slime = npcList.get(0);
			assertThat(slime.getUnlocked()).isTrue();
			assertThat(slime.getWinProbability()).isEqualTo(50);

			NpcDto goblin = npcList.get(1);
			assertThat(goblin.getUnlocked()).isFalse();
			assertThat(goblin.getWinProbability()).isEqualTo(0);
		}

		@Test
		@DisplayName("レベル差が大きいほど勝率は上がるが上限90%で頭打ちになる")
		void getNpcList_highLevel_probabilityCapped() {
			mockLevel(100);

			List<NpcDto> npcList = battleServiceWithRoll(1).getNpcList(CHILD_USER_ID);

			assertThat(npcList).allMatch(NpcDto::getUnlocked);
			assertThat(npcList).allMatch(npc -> npc.getWinProbability() <= 90);
			// DRAGON(requiredLevel=70)はlevel差30なので 50+30*5=200 -> 90に頭打ち
			NpcDto dragon = npcList.get(npcList.size() - 1);
			assertThat(dragon.getWinProbability()).isEqualTo(90);
		}
	}

	@Nested
	@DisplayName("challenge")
	class Challenge {

		@Test
		@DisplayName("存在しないNPCコードの場合はNoSuchElementExceptionを投げる")
		void challenge_unknownNpc_throws() {
			mockLevel(50);

			assertThatThrownBy(() -> battleServiceWithRoll(1).challenge(CHILD_USER_ID, "UNKNOWN"))
					.isInstanceOf(NoSuchElementException.class);
		}

		@Test
		@DisplayName("必要レベル未満のNPCに挑戦するとIllegalArgumentExceptionを投げる")
		void challenge_belowRequiredLevel_throws() {
			mockLevel(5);

			assertThatThrownBy(() -> battleServiceWithRoll(1).challenge(CHILD_USER_ID, "GOBLIN"))
					.isInstanceOf(IllegalArgumentException.class);

			verify(characterService, never()).addCoins(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
		}

		@Test
		@DisplayName("乱数が勝率以下なら勝利し、コインが加算される")
		void challenge_winRoll_awardsCoins() {
			mockLevel(0); // SLIME: requiredLevel=0, winProbability=50, coinReward=5

			BattleResultDto result = battleServiceWithRoll(50).challenge(CHILD_USER_ID, "SLIME");

			assertThat(result.getWin()).isTrue();
			assertThat(result.getCoinsAwarded()).isEqualTo(5);
			verify(characterService).addCoins(CHILD_USER_ID, 5);
		}

		@Test
		@DisplayName("乱数が勝率を超えたら敗北し、コインは加算されない")
		void challenge_loseRoll_noCoins() {
			mockLevel(0); // SLIME: winProbability=50

			BattleResultDto result = battleServiceWithRoll(51).challenge(CHILD_USER_ID, "SLIME");

			assertThat(result.getWin()).isFalse();
			assertThat(result.getCoinsAwarded()).isEqualTo(0);
			verify(characterService, never()).addCoins(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
		}
	}
}
